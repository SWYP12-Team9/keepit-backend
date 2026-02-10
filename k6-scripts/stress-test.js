import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site';
const USERNAME = __ENV.USERNAME || 'test@test.com';
const PASSWORD = __ENV.PASSWORD || 'password123';

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

function login() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  check(res, {
    'login: status 200': (r) => r.status === 200,
  });

  try {
    const body = JSON.parse(res.body);
    return body.accessToken || '';
  } catch {
    return '';
  }
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  const token = login();
  if (!token) {
    sleep(1);
    return;
  }
  const params = authHeaders(token);

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
