import { check } from 'k6';
import exec from 'k6/execution';

import {
  arrivalRateScenario,
  envInteger,
  envString,
  outcomeThresholds,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { postJson, tryJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const SCENARIO = 'order_throughput';
const METRIC_PREFIX = 'order';
const productId = envInteger('ORDER_PRODUCT_ID', 1, 1);
const quantity = envInteger('ORDER_QUANTITY', 1, 1);
const customerPrefix = envString('ORDER_CUSTOMER_PREFIX', 'k6-customer').slice(0, 24);
const metrics = createOutcomeMetrics(METRIC_PREFIX);

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('ORDER', {
        startRate: 1,
        peakRate: 2,
        rampUp: '15s',
        steady: '30s',
        rampDown: '15s',
        preAllocatedVUs: 20,
        maxVUs: 100,
      }),
      exec: 'orderThroughput',
      tags: { lab: 'order-throughput', operation: 'create-order' },
    },
  },
  thresholds: outcomeThresholds(METRIC_PREFIX.toUpperCase(), SCENARIO, {
    maxErrorRate: 0.01,
    p95Ms: 1000,
    p99Ms: 2000,
  }),
  summaryTrendStats: SUMMARY_TREND_STATS,
};

function requestId() {
  return `k6-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
}

export function orderThroughput() {
  const id = requestId();
  const response = postJson(
    urlFor('/api/orders'),
    {
      customerName: `${customerPrefix}-${exec.vu.idInTest}`.slice(0, 40),
      lines: [{ productId, quantity }],
    },
    { name: 'POST /api/orders', operation: 'create-order' },
    {
      'Idempotency-Key': id,
      'X-Load-Test-Request-Id': id,
    },
  );
  const body = tryJson(response);
  const successful = check(response, {
    'order is accepted': (result) => [200, 201, 202].includes(result.status),
    'order response has no server error': (result) => result.status < 500,
    'synchronous order response contains order data': (result) =>
      result.status === 202 ||
      (body !== null && typeof body === 'object' && body.data !== null && typeof body.data === 'object'),
  });

  recordOutcome(metrics, successful, { operation: 'create-order' });
}

export function handleSummary(data) {
  return summaryOutputs('order-throughput', data);
}
