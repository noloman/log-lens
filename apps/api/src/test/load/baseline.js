import http from 'k6/http';
  import { check, sleep } from 'k6';

  // This defines your load profile
  export const options = {
    stages: [
      { duration: '30s', target: 10 },   // ramp up to 10 virtual users
      { duration: '1m',  target: 10 },   // hold at 10 VUs
      { duration: '30s', target: 50 },   // ramp up to 50
      { duration: '1m',  target: 50 },   // hold at 50
      { duration: '30s', target: 0 },    // ramp down
    ],
    thresholds: {
      http_req_duration: ['p(95)<500'],   // 95% of requests under 500ms
      http_req_failed: ['rate<0.01'],     // <1% failure rate
    },
  };

  const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

  // You'll need a valid JWT — get one in setup()
  export function setup() {
    const loginRes = http.post(`${BASE_URL}/v1/auth/login`,
      JSON.stringify({ username: 'loadtest', password: 'loadtest123' }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    return { token: loginRes.json('token') };
  }

  // This runs once per VU per iteration
  export default function (data) {
    const headers = {
      Authorization: `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    };

    // Test GET /v1/logs
    const getRes = http.get(`${BASE_URL}/v1/logs`, { headers });
    check(getRes, {
      'GET /logs status 200': (r) => r.status === 200,
      'GET /logs duration < 500ms': (r) => r.timings.duration < 500,
    });

    // Test POST /v1/logs
    const payload = JSON.stringify({
      serviceName: 'k6-load-test',
      level: 'INFO',
      message: `Load test entry ${Date.now()}`,
    });
    const postRes = http.post(`${BASE_URL}/v1/logs`, payload, { headers });
    check(postRes, {
      'POST /logs status 201': (r) => r.status === 201,
    });

    sleep(1); // 1 second think time between iterations
  }