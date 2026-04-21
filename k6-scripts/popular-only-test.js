import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://api.sor999.site';
const USERNAME = __ENV.USERNAME || 'user1';
const PASSWORD = __ENV.PASSWORD || 'password';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up: 0 → 50 VU (트래픽 순간 폭주 시뮬레이션)
    { duration: '1m', target: 50 },   // Steady: 50 VU (기존보다 더 가혹한 조건)
    { duration: '10s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    // 캐시가 적용되었으므로 p(95)가 200ms 이하로 떨어져야 정상입니다!
    http_req_duration: ['p(95)<200'], 
    http_req_failed: ['rate<0.01'],
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

  // 캐시가 적용된 오직 "인기글 탭"만 집중적으로 때립니다!
  const popRes = http.get(`${BASE_URL}/api/v1/recommendations/popular?size=20`, params);
  
  check(popRes, {
    'popular links (cached): status 200': (r) => r.status === 200,
  });

  // 다른 API 호출 없이 유저가 잠깐 쉬었다가 미친듯이 새로고침한다고 가정
  sleep(Math.random() * 0.5); 
}
