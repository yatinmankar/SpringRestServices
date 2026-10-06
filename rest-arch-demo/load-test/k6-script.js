// 10k req/s load test for POST /api/v1/orders.
//
// Traffic is spread across many synthetic client IDs (each gets its own
// rate-limit bucket server-side), so this exercises the full pipeline -
// auth, rate limiting, validation, idempotency, risk check, async persist -
// rather than just tripping one client's limit. Bump NUM_CLIENTS or
// app.rate-limit.* in application.yml if you change the target rate.
//
// Install k6: https://k6.io/docs/get-started/installation/
// Run:
//   k6 run load-test/k6-script.js
//   k6 run -e BASE_URL=http://your-host:8080 load-test/k6-script.js

import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const NUM_CLIENTS = Number(__ENV.NUM_CLIENTS || 50);

export const options = {
  scenarios: {
    ramp_to_10k_rps: {
      executor: 'ramping-arrival-rate',
      startRate: 500,
      timeUnit: '1s',
      preAllocatedVUs: 2000,
      maxVUs: 8000,
      stages: [
        { target: 2000, duration: '15s' },
        { target: 5000, duration: '20s' },
        { target: 10000, duration: '30s' },
        { target: 10000, duration: '30s' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<500'],
  },
};

// Runs once before the test: mint one JWT per synthetic client so the VUs
// below aren't all hammering a single rate-limit bucket.
export function setup() {
  const tokens = [];
  for (let i = 0; i < NUM_CLIENTS; i++) {
    const clientId = `loadtest-client-${i}`;
    const res = http.post(`${BASE_URL}/api/v1/auth/token?clientId=${clientId}`);
    const token = res.json('token');
    if (!token) {
      throw new Error(`failed to obtain token for ${clientId}: ${res.status} ${res.body}`);
    }
    tokens.push(token);
  }
  return { tokens };
}

export default function (data) {
  const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];

  const payload = JSON.stringify({
    instrumentId: 'AAPL',
    quantity: Math.ceil(Math.random() * 100),
    price: Number((100 + Math.random() * 10).toFixed(2)),
    side: Math.random() > 0.5 ? 'BUY' : 'SELL',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      'Idempotency-Key': uuidv4(),
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);

  check(res, {
    'status is 200/202/429/503': (r) => [200, 202, 429, 503].includes(r.status),
  });
}
