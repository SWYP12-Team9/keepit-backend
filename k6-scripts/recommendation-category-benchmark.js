import http from 'k6/http';
import {check, group, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL;
const USERNAME = __ENV.USERNAME || 'testuser';
const PASSWORD = __ENV.PASSWORD || 'password123';
const CATEGORY = __ENV.CATEGORY || '경제/시사';
const USE_AUTH = (__ENV.USE_AUTH || 'true') === 'true';
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || '60s';
const WARMUP_REQUESTS = Number(__ENV.WARMUP_REQUESTS || 1);
const SLEEP_MS = Number(__ENV.SLEEP_MS || 0);

// 카테고리별 응답시간 측정 (캐시 히트 전후 비교용)
const durationAll = new Trend('rec_category_duration_ms', true);
const failures = new Counter('rec_category_failures');

// 8개 고정 카테고리 (캐싱 효과 검증 대상)
const ALL_CATEGORIES = [
  '경제/시사',
  '뷰티/패션',
  '요리/식품',
  '운동/건강',
  '인문/지식',
  '직장/자기개발',
  '홈/리빙',
  '기타'
];

export const options = {
  stages: [
    { duration: '15s', target: VUS },     // Ramp-up
    { duration: DURATION, target: VUS },  // Steady
    { duration: '10s', target: 0 },       // Ramp-down
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    rec_category_duration_ms: ['p(95)<3000'],
  },
};

export function setup() {
  if (!BASE_URL) {
    throw new Error('BASE_URL env is required');
  }

  let accessToken = '';

  if (USE_AUTH) {
    const loginRes = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ username: USERNAME, password: PASSWORD }),
      { headers: { 'Content-Type': 'application/json' } },
    );

    const loginOk = check(loginRes, {
      'setup login: status 200': (r) => r.status === 200,
    });

    if (!loginOk) {
      throw new Error(`login failed: ${loginRes.status} ${loginRes.body}`);
    }

    accessToken = JSON.parse(loginRes.body).accessToken;
  }

  const headers = buildHeaders(accessToken);

  // 워밍업: 각 카테고리를 미리 호출해 Redis 캐시를 채움
  // WARMUP_REQUESTS=0 이면 워밍업 없이 시작한다. 캐시 적용 브랜치에서는 첫 카테고리 요청 이후 캐시가 채워질 수 있다.
  // WARMUP_REQUESTS=1 이상이면 steady 구간 전에 주요 카테고리 캐시를 채운다.
  if (WARMUP_REQUESTS > 0) {
    console.log(`[setup] 캐시 워밍업 시작 (${ALL_CATEGORIES.length}개 카테고리 × ${WARMUP_REQUESTS}회)`);
    for (let i = 0; i < WARMUP_REQUESTS; i++) {
      ALL_CATEGORIES.forEach((cat) => {
        http.get(
          `${BASE_URL}/api/v1/recommendations?category=${encodeURIComponent(cat)}&size=10`,
          { headers, tags: { phase: 'warmup' } },
        );
      });
    }
    console.log('[setup] 캐시 워밍업 완료');
  } else {
    console.log('[setup] WARMUP_REQUESTS=0 → cold cache 상태로 테스트 시작');
  }

  return { headers };
}

export default function (data) {
  // 8개 카테고리 중 랜덤 선택 (실제 사용 패턴 시뮬레이션)
  const category = ALL_CATEGORIES[Math.floor(Math.random() * ALL_CATEGORIES.length)];
  const encodedCategory = encodeURIComponent(category);

  group('카테고리 추천 조회', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/recommendations?category=${encodedCategory}&size=10`,
      {
        headers: data.headers,
        tags: { endpoint: 'recommendation-category', category },
      },
    );

    durationAll.add(res.timings.duration, { category });

    const ok = check(res, {
      'status 200': (r) => r.status === 200,
      'has contents': (r) => {
        try {
          return JSON.parse(r.body)?.data?.contents !== undefined;
        } catch {
          return false;
        }
      },
    });

    if (!ok) {
      failures.add(1);
      console.error(`[FAIL] category=${category} status=${res.status}`);
    }
  });

  if (SLEEP_MS > 0) {
    sleep(SLEEP_MS / 1000);
  }
}

function buildHeaders(accessToken) {
  const headers = { 'Content-Type': 'application/json' };
  if (USE_AUTH && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }
  return headers;
}
