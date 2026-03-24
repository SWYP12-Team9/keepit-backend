import http from 'k6/http';
import {check, group, sleep} from 'k6';

//const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // 로컬
const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site'; // 서버
const USERNAME = __ENV.USERNAME || 'testuser'; // 테스트용 계정 아이디
const PASSWORD = __ENV.PASSWORD || 'password123'; // 테스트용 계정 비밀번호
const TEST_LINK_URL = __ENV.TEST_LINK_URL || 'https://www.youtube.com/watch?v=cam0qMyR4Qg&t=403s'; // 링크 생성 테스트용 URL

// 부하 테스트 설정
// duration: 테스트 지속시간
// target : duration이 끝나는 시점에 도달해 있어야 할 가상 사용자 수
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp-up: 0 → 20 VU (테스트 시작 시점(0명)부터 30초 동안 사용자를 20명까지 천천히 늘림)
    { duration: '1m', target: 20 },   // Steady: 20 VU 유지 (이미 20명에 도달했으므로, 다음 1분 동안 사용자 20명을 유지)
    { duration: '10s', target: 0 },   // Ramp-down: 20 → 0 VU (마지막 10초 동안 20명에서 0명으로 사용자를 서서히 줄이며 테스트를 종료)
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

// 테스트 시작 전 1회 실행 — 로그인하여 토큰을 모든 VU에 공유
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const loginSuccess = check(res, {
    'setup login: status 200': (r) => r.status === 200,
  });

  if (!loginSuccess) {
    console.error(`Login failed in setup: status=${res.status}, body=${res.body}`);
    return { accessToken: '', refreshToken: '' };
  }

  const body = JSON.parse(res.body);
  console.log('Setup login successful, token acquired');
  return {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
  };
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

// data는 setup()에서 반환한 토큰 객체
export default function (data) {
  if (!data.accessToken) {
    console.error('No access token available, skipping iteration');
    sleep(1);
    return;
  }
  const params = authHeaders(data.accessToken);

  // 1. 프로필 조회
  group('User - 프로필 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/users/info`, params);
    check(res, { 'profile: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 2. 레퍼런스 목록 조회
  group('Reference - 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/references`, params);
    check(res, { 'references list: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 3. 자주 찾는 레퍼런스 조회
  group('Reference - 자주 찾는 레퍼런스', () => {
    const res = http.get(`${BASE_URL}/api/v1/references/frequent`, params);
    check(res, { 'frequent refs: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 4. 링크 목록 조회
  group('UserLink - 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/user-links`, params);
    check(res, { 'links list: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 5. 링크 검색
  group('UserLink - 검색', () => {
    const res = http.get(`${BASE_URL}/api/v1/user-links/search?keyword=test&size=20`, params);
    check(res, { 'link search: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 6. 카테고리 목록 조회
  group('Recommendation - 카테고리 목록', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations/categories`, params);
    check(res, { 'categories: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 7. 키워드 검색 (추천)
  group('Recommendation - 키워드 검색', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations/search?keyword=test`, params);
    check(res, { 'rec search: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 8. 카테고리별 추천 조회
  group('Recommendation - 카테고리별 추천', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations?category=${encodeURIComponent('경제/시사')}`, params);
    check(res, { 'rec by category: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 9. 사용자 통계 조회
  group('Stat - 사용자 통계', () => {
    const res = http.get(`${BASE_URL}/api/v1/users/stats`, params);
    check(res, { 'user stats: status 200': (r) => r.status === 200 });
  });
  sleep(0.5);

  // 쓰기 API — 낮은 비율로 실행 (약 20% 확률)
  if (Math.random() < 0.2) {
    group('Reference - 생성', () => {
      const payload = JSON.stringify({
        title: `k6-test-ref-${Date.now()}`,
        isPublic: true,
      });
      const res = http.post(`${BASE_URL}/api/v1/references`, payload, params);
      check(res, { 'create reference: status 2xx': (r) => r.status >= 200 && r.status < 300 });
    });
    sleep(0.3);
  }

  if (Math.random() < 0.2) {
    group('UserLink - 생성', () => {
      const payload = JSON.stringify({
        url: `${TEST_LINK_URL}?k6=${__VU}${__ITER}${Date.now()}`,
      });
      const res = http.post(`${BASE_URL}/api/v1/user-links`, payload, params);
      check(res, { 'create link: status 2xx': (r) => r.status >= 200 && r.status < 300 });
    });
    sleep(0.3);
  }

  // 토큰 재발급 테스트 (약 10% 확률)
  if (Math.random() < 0.1 && data.refreshToken) {
    group('Auth - 토큰 재발급', () => {
      const res = http.post(
        `${BASE_URL}/api/v1/jwt/refresh`,
        JSON.stringify({ refreshToken: data.refreshToken }),
        { headers: { 'Content-Type': 'application/json' } },
      );
      check(res, { 'token refresh: status 200': (r) => r.status === 200 });
    });
    sleep(0.3);
  }

  sleep(1);
}
