import { check } from 'k6';

import {
  arrivalRateScenario,
  envInteger,
  envString,
  outcomeThresholds,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { getJson, tryJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const SCENARIO = 'hot_product_burst';
const METRIC_PREFIX = 'hot_product';
const productId = envInteger('HOT_PRODUCT_ID', 1, 1);
const targetPath = envString('HOT_PRODUCT_PATH', `/api/products/${productId}`).replace(
  '{productId}',
  String(productId),
);
const metrics = createOutcomeMetrics(METRIC_PREFIX);

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('HOT_PRODUCT', {
        startRate: 20,
        peakRate: 250,
        rampUp: '10s',
        steady: '45s',
        rampDown: '10s',
        preAllocatedVUs: 100,
        maxVUs: 500,
      }),
      exec: 'hotProductBurst',
      tags: { lab: 'cache-stampede', operation: 'get-hot-product' },
    },
  },
  thresholds: outcomeThresholds(METRIC_PREFIX.toUpperCase(), SCENARIO, {
    maxErrorRate: 0.01,
    p95Ms: 750,
    p99Ms: 1500,
  }),
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function hotProductBurst() {
  const response = getJson(urlFor(targetPath), {
    name: 'GET /api/products/:id',
    operation: 'get-hot-product',
  });
  const body = tryJson(response);
  const data = body !== null && typeof body === 'object' && body.data !== undefined ? body.data : body;
  const product =
    data !== null && typeof data === 'object' && data.product !== undefined ? data.product : data;
  const successful = check(response, {
    'hot product status is 200': (result) => result.status === 200,
    'hot product body is JSON object': () =>
      product !== null && typeof product === 'object' && !Array.isArray(product),
    'hot product id matches': () => product !== null && Number(product.id) === productId,
  });

  recordOutcome(metrics, successful, { operation: 'get-hot-product' });
}

export function handleSummary(data) {
  return summaryOutputs('hot-product-burst', data);
}
