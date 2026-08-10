import { check } from 'k6';

import {
  arrivalRateScenario,
  outcomeThresholds,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { getJson, tryJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const SCENARIO = 'catalog_read';
const METRIC_PREFIX = 'catalog_read';
const metrics = createOutcomeMetrics(METRIC_PREFIX);

function isProductCollection(body) {
  return (
    Array.isArray(body) ||
    (body !== null &&
      typeof body === 'object' &&
      (Array.isArray(body.data) ||
        Array.isArray(body.products) ||
        Array.isArray(body.items) ||
        Array.isArray(body.content)))
  );
}

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('CATALOG_READ', {
        startRate: 5,
        peakRate: 50,
        rampUp: '30s',
        steady: '2m',
        rampDown: '30s',
        preAllocatedVUs: 50,
        maxVUs: 200,
      }),
      exec: 'catalogRead',
      tags: { lab: 'catalog-read-baseline', operation: 'list-products' },
    },
  },
  thresholds: outcomeThresholds(METRIC_PREFIX.toUpperCase(), SCENARIO, {
    maxErrorRate: 0.01,
    p95Ms: 500,
    p99Ms: 1000,
  }),
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function catalogRead() {
  const response = getJson(urlFor('/api/products'), {
    name: 'GET /api/products',
    operation: 'list-products',
  });
  const body = tryJson(response);
  const successful = check(response, {
    'catalog status is 200': (result) => result.status === 200,
    'catalog body is a product collection': () => isProductCollection(body),
  });

  recordOutcome(metrics, successful, { operation: 'list-products' });
}

export function handleSummary(data) {
  return summaryOutputs('catalog-read-baseline', data);
}
