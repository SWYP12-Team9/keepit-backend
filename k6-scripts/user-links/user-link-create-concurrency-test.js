import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,
    iterations: 10,
};

const ACCESS_TOKEN = __ENV.TOKEN;
const HOST = __ENV.HOST;

export default function () {
    const url = 'https://www.scrapethissite.com/pages/forms/?page_num=99997';

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
