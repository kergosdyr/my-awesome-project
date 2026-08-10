import { Counter, Rate } from 'k6/metrics';

export function createOutcomeMetrics(prefix) {
  return {
    successTotal: new Counter(`${prefix}_success_total`),
    errorTotal: new Counter(`${prefix}_error_total`),
    successRate: new Rate(`${prefix}_success_rate`),
    errorRate: new Rate(`${prefix}_error_rate`),
  };
}

export function recordOutcome(metrics, successful, tags = {}) {
  metrics.successRate.add(successful, tags);
  metrics.errorRate.add(!successful, tags);

  if (successful) {
    metrics.successTotal.add(1, tags);
  } else {
    metrics.errorTotal.add(1, tags);
  }
}
