import { check } from 'k6';
import exec from 'k6/execution';

import {
  arrivalRateScenario,
  envInteger,
  envString,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { postJson, tryJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const SCENARIO = 'kafka_outbox_orders';
const strategy = envString('EVENT_STRATEGY', 'outbox').toLowerCase();
const productId = envInteger('EVENT_LAB_PRODUCT_ID', 1, 1);
const quantity = envInteger('EVENT_LAB_QUANTITY', 1, 1);
const metrics = createOutcomeMetrics(`event_lab_${strategy}`);

if (!['direct', 'outbox'].includes(strategy)) {
  throw new Error(`EVENT_STRATEGY must be direct or outbox; received "${strategy}"`);
}

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('EVENT_LAB', {
        startRate: 1,
        peakRate: 2,
        rampUp: '15s',
        steady: '30s',
        rampDown: '15s',
        preAllocatedVUs: 20,
        maxVUs: 100,
      }),
      exec: 'placeLabOrder',
      tags: { lab: 'kafka-outbox', strategy, operation: 'create-event-order' },
    },
  },
  thresholds: {
    [`http_req_duration{scenario:${SCENARIO}}`]: [
      `p(95)<${envInteger('EVENT_LAB_P95_MS', 1500, 1)}`,
      `p(99)<${envInteger('EVENT_LAB_P99_MS', 3000, 1)}`,
    ],
    [`event_lab_${strategy}_success_rate`]: ['rate>0.99'],
    [`event_lab_${strategy}_error_rate`]: ['rate<0.01'],
  },
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function placeLabOrder() {
  const response = postJson(
    urlFor(`/api/labs/events/orders?strategy=${strategy}`),
    {
      customerName: `event-lab-${exec.vu.idInTest}`,
      lines: [{ productId, quantity }],
    },
    {
      name: 'POST /api/labs/events/orders',
      operation: 'create-event-order',
      strategy,
    },
  );
  const body = tryJson(response);
  const successful = check(response, {
    'lab order status is 201': (result) => result.status === 201,
    'lab order response strategy matches': () =>
      body !== null &&
      body.data !== null &&
      String(body.data.strategy).toLowerCase() === strategy,
    'lab order response has event id': () =>
      body !== null && body.data !== null && typeof body.data.eventId === 'string',
  });

  recordOutcome(metrics, successful, { strategy });
}

export function handleSummary(data) {
  return summaryOutputs(`kafka-outbox-${strategy}`, data);
}
