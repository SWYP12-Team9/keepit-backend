import http from 'k6/http';
import {check, group, sleep} from 'k6';

//const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // 로컬
const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site'; // 서버
const USERNAME = __ENV.USERNAME || 'testuser'; // 테스트용 계정 아이디
const PASSWORD = __ENV.PASSWORD || 'password123'; // 테스트용 계정 비밀번호

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 0 → 10 VU
    { duration: '30s', target: 30 },   // 10 → 30 VU
    { duration: '30s', target: 50 },   // 30 → 50 VU
    { duration: '1m', target: 50 },    // 50 VU 유지
    { duration: '30s', target: 0 },    // 50 → 0 VU
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.1'],
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
    return { accessToken: '' };
  }

  const body = JSON.parse(res.body);
  console.log('Setup login successful, token acquired');
  return {
    accessToken: body.accessToken,
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

  group('User - 프로필 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/users/info`, params);
    check(res, { 'profile: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Reference - 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/references`, params);
    check(res, { 'references list: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Reference - 자주 찾는 레퍼런스', () => {
    const res = http.get(`${BASE_URL}/api/v1/references/frequent`, params);
    check(res, { 'frequent refs: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('UserLink - 목록 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/user-links`, params);
    check(res, { 'links list: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('UserLink - 검색', () => {
    const res = http.get(`${BASE_URL}/api/v1/user-links/search?keyword=test&size=20`, params);
    check(res, { 'link search: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Recommendation - 카테고리 목록', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations/categories`, params);
    check(res, { 'categories: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Recommendation - 키워드 검색', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations/search?keyword=test`, params);
    check(res, { 'rec search: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Recommendation - 카테고리별 추천', () => {
    const res = http.get(`${BASE_URL}/api/v1/recommendations?category=${encodeURIComponent('경제/시사')}`, params);
    check(res, { 'rec by category: status 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  group('Stat - 사용자 통계', () => {
    const res = http.get(`${BASE_URL}/api/v1/users/stats`, params);
    check(res, { 'user stats: status 200': (r) => r.status === 200 });
  });

  sleep(0.5);
}
