import http from 'k6/http';
import { check, sleep } from 'k6';


// URL 선정: 정적 HTML url, 크롤링 테스트 가능한 url로 진행. 스크래퍼 서버에서 <title> 보장, body content 풍부, Playwright 로직 타지 않음
// - VU1: page=PAGE_OFFSET+1~PAGE_OFFSET+1000, VU2: PAGE_OFFSET+1001~PAGE_OFFSET+2000, ...
// - PAGE_OFFSET 환경변수로 이전 테스트와 겹치지 않는 URL 범위 지정 (기본값: 30000)

export const options = {
    vus: 10,
    duration: '5m',
    thresholds: {
        'http_req_duration': ['p(95)<15000'],
        'http_req_failed': ['rate<0.05'],
    },
};

const ACCESS_TOKEN = __ENV.TOKEN;
const HOST = __ENV.HOST;
const PAGE_OFFSET = parseInt(__ENV.PAGE_OFFSET || '30000');

export default function () {
    // VU별 고유 page 범위로 URL 충돌 방지 (항상 신규 URL → 스크래핑 + AI 요약 발동)
    const page = PAGE_OFFSET + (__VU - 1) * 1000 + __ITER + 1;
    const url = `https://www.scrapethissite.com/pages/forms/?page_num=${page}`;

    const payload = JSON.stringify({
        url: url,
        referenceId: 96,
    });

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };

    const res = http.post(`${HOST}/api/v1/user-links`, payload, { headers });

    check(res, {
        'status is 201': (r) => r.status === 201,
        'no server error (5xx)': (r) => r.status < 500,
    });

    sleep(1);
}
