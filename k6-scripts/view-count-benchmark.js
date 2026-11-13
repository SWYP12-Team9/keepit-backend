import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site';
const USERNAME = __ENV.USERNAME || 'testuser';
const PASSWORD = __ENV.PASSWORD || 'password123';
const LINK_IDS_STR = __ENV.LINK_IDS || '1,2,3,4,5,6,7,8,9,10';
const LINK_IDS = LINK_IDS_STR.split(',').map(Number);

const viewDuration = new Trend('view_increment_duration_ms', true);
const viewFailures = new Counter('view_increment_failures');

export const options = {
  stages: [
    { duration: '15s', target: 50 },   // Ramp-up: 0 → 50 VU
    { duration: '60s', target: 50 },   // Steady: 50 VU
    { duration: '10s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const loginOk = check(loginRes, {
    'login: status 200': (r) => r.status === 200,
  });

  if (!loginOk) {
    throw new Error(`login failed: ${loginRes.status} ${loginRes.body}`);
  }

  const accessToken = JSON.parse(loginRes.body).accessToken;
  console.log(`Login success, token acquired. Target links: ${LINK_IDS}`);

  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function (data) {
  if (!data.headers) {
    sleep(1);
    return;
  }

  // 랜덤 링크 선택 (실제 트래픽 분포 시뮬레이션)
  const linkId = LINK_IDS[Math.floor(Math.random() * LINK_IDS.length)];

  // 공개 조회수 증가 API 호출
  const res = http.post(
    `${BASE_URL}/api/v1/recommendations/links/${linkId}/view`,
    null,
    { headers: data.headers },
  );

  viewDuration.add(res.timings.duration, { linkId: String(linkId) });

  const ok = check(res, {
    'view count: status 200': (r) => r.status === 200,
  });

  if (!ok) {
    viewFailures.add(1);
    console.error(`[FAIL] linkId=${linkId} status=${res.status}`);
  }

  sleep(Math.random() * 0.3);
}
