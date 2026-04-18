import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site';
const USERNAME = __ENV.USERNAME || 'user1';
const PASSWORD = __ENV.PASSWORD || 'password';

export const options = {
  stages: [
    { duration: '30s', target: 30 },  // Ramp-up: 0 → 30 VU (빠른 트래픽 유입)
    { duration: '1m', target: 30 },   // Steady: 30 VU (인기글 탭 탐색 메인 트래픽)
    { duration: '10s', target: 0 },   // Ramp-down: 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 인기글 탭이므로 더 빡빡한 기준 (1초 이하)
    http_req_failed: ['rate<0.01'],    // 실패율 1% 미만
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginSuccess = check(res, {
    'setup login: status 200': (r) => r.status === 200,
  });

  if (!loginSuccess) {
    console.error(`Login failed: status=${res.status}`);
    return { accessToken: '' };
  }

  const body = JSON.parse(res.body);
  return { accessToken: body.accessToken };
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function (data) {
  if (!data.accessToken) {
    sleep(1);
    return;
  }
  const params = authHeaders(data.accessToken);

  let firstRecommendationLinkId = null;

  group('인기글 탭 (Popular) 진입 및 데이터 로드', () => {

    // 1. 전역 시스템 인기순 조회 (DB 기반 쿼리 확인)
    const popRes = http.get(`${BASE_URL}/api/v1/recommendations/popular?size=20`, params);
    const popCheck = check(popRes, {
      'popular links: status 200': (r) => r.status === 200,
    });

    if (popCheck) {
      const popData = popRes.json();
      if (popData?.data?.contents?.[0]?.linkId) {
        firstRecommendationLinkId = popData.data.contents[0].linkId;
      }
    }
    sleep(0.5); // 읽으면서 잠시 대기

    // 2. 카테고리별 추천글 조회 (Elasticsearch 기반 검색 쿼리 확인)
    const categories = ['경제/시사', '뷰티/패션', '요리/식품', '운동/건강', '인문/지식', '직장/자기개발', '홈/리빙', '기타'];
    const randomCategory = categories[Math.floor(Math.random() * categories.length)];
    const encodedCategory = encodeURIComponent(randomCategory);
    const recRes = http.get(`${BASE_URL}/api/v1/recommendations?category=${encodedCategory}&size=20`, params);
    check(recRes, {
      'category recommendations: status 200': (r) => r.status === 200,
    });
    sleep(0.5);

    // 3. 내 자주 찾는 레퍼런스 (개인별 인기글 확인)
    const myFreqRes = http.get(`${BASE_URL}/api/v1/references/frequent?size=20`, params);
    check(myFreqRes, {
      'my frequent references: status 200': (r) => r.status === 200,
    });
  });

  // 4. 인기글 중 하나를 클릭해서 조회수 증가시킴 (쓰기 작업)
  if (firstRecommendationLinkId) {
    group('인기글 클릭 (조회수 증가)', () => {
      // 30%의 유저만 실제로 클릭한다고 가정
      if (Math.random() < 0.3) {
        const viewRes = http.post(`${BASE_URL}/api/v1/recommendations/links/${firstRecommendationLinkId}/view`, '{}', params);
        check(viewRes, {
          'increment view: status 200': (r) => r.status === 200,
        });
      }
    });
  }

  // 화면 스크롤 하거나 정독하는 유저 체류 시간
  sleep(Math.random() * 2 + 1); 
}
