import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

import {
  arrivalRateScenario,
  envInteger,
  envString,
  outcomeThresholds,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { requestJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const SCENARIO = 'flash_sale_admission';
const METRIC_PREFIX = 'admission';
const method = envString('WAITING_ROOM_METHOD', 'POST').toUpperCase();
const path = envString('WAITING_ROOM_PATH', '/api/flash-sales/1/admission');
const productId = envInteger('WAITING_ROOM_PRODUCT_ID', 1, 1);
const quantity = envInteger('WAITING_ROOM_QUANTITY', 1, 1);
const expectedStatuses = envString('WAITING_ROOM_EXPECTED_STATUSES', '200,201,202,429')
  .split(',')
  .map((status) => Number(status.trim()));
const metrics = createOutcomeMetrics(METRIC_PREFIX);
const admittedTotal = new Counter('admission_admitted_total');
const queuedTotal = new Counter('admission_queued_total');
const throttledTotal = new Counter('admission_throttled_total');

if (!['GET', 'POST', 'PUT', 'PATCH'].includes(method)) {
  throw new Error(`WAITING_ROOM_METHOD must be GET, POST, PUT, or PATCH; received "${method}"`);
}

if (expectedStatuses.length === 0 || expectedStatuses.some((status) => !Number.isInteger(status))) {
  throw new Error('WAITING_ROOM_EXPECTED_STATUSES must be a comma-separated list of HTTP status codes');
}

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('WAITING_ROOM', {
        startRate: 50,
        peakRate: 500,
        rampUp: '15s',
        steady: '1m',
        rampDown: '15s',
        preAllocatedVUs: 200,
        maxVUs: 1000,
      }),
      exec: 'flashSaleAdmission',
      tags: { lab: 'waiting-room', operation: 'flash-sale-admission' },
    },
  },
  thresholds: outcomeThresholds(METRIC_PREFIX.toUpperCase(), SCENARIO, {
    maxErrorRate: 0.01,
    p95Ms: 500,
    p99Ms: 1000,
  }),
  summaryTrendStats: SUMMARY_TREND_STATS,
};

function requestId() {
  return `waiting-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
}

export function flashSaleAdmission() {
  const id = requestId();
  const response = requestJson(
    method,
    urlFor(path),
    {
      customerId: `load-customer-${exec.vu.idInTest}`,
      productId,
      quantity,
    },
    { name: `${method} waiting-room admission`, operation: 'flash-sale-admission' },
    {
      'Idempotency-Key': id,
      'X-Load-Test-Request-Id': id,
    },
  );
  const expected = expectedStatuses.includes(response.status);
  const successful = check(response, {
    'admission returns an expected control response': () => expected,
    'admission response has no server error': (result) => result.status < 500,
  });

  if (response.status === 200 || response.status === 201) {
    admittedTotal.add(1);
  } else if (response.status === 202) {
    queuedTotal.add(1);
  } else if (response.status === 429) {
    throttledTotal.add(1);
  }

  recordOutcome(metrics, successful, { operation: 'flash-sale-admission' });
}

export function handleSummary(data) {
  return summaryOutputs('flash-sale-admission', data);
}
