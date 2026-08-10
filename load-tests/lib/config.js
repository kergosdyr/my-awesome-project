const DEFAULT_BASE_URL = 'http://localhost:8080';

function envValue(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === '' ? fallback : value;
}

export function envInteger(name, fallback, minimum = 0) {
  const raw = envValue(name, String(fallback));
  const parsed = Number(raw);

  if (!Number.isInteger(parsed) || parsed < minimum) {
    throw new Error(`${name} must be an integer greater than or equal to ${minimum}; received "${raw}"`);
  }

  return parsed;
}

export function envNumber(name, fallback, minimum = 0) {
  const raw = envValue(name, String(fallback));
  const parsed = Number(raw);

  if (!Number.isFinite(parsed) || parsed < minimum) {
    throw new Error(`${name} must be a number greater than or equal to ${minimum}; received "${raw}"`);
  }

  return parsed;
}

export function envString(name, fallback) {
  return envValue(name, fallback);
}

export function baseUrl() {
  return envString('BASE_URL', DEFAULT_BASE_URL).replace(/\/+$/, '');
}

export function urlFor(path) {
  if (/^https?:\/\//.test(path)) {
    return path;
  }

  return `${baseUrl()}/${path.replace(/^\/+/, '')}`;
}

export function arrivalRateScenario(prefix, defaults) {
  const startRate = envInteger(`${prefix}_START_RATE`, defaults.startRate, 0);
  const peakRate = envInteger(`${prefix}_PEAK_RATE`, defaults.peakRate, 1);
  const preAllocatedVUs = envInteger(
    `${prefix}_PRE_ALLOCATED_VUS`,
    defaults.preAllocatedVUs,
    1,
  );
  const maxVUs = envInteger(`${prefix}_MAX_VUS`, defaults.maxVUs, 1);

  if (maxVUs < preAllocatedVUs) {
    throw new Error(
      `${prefix}_MAX_VUS (${maxVUs}) must be greater than or equal to ` +
        `${prefix}_PRE_ALLOCATED_VUS (${preAllocatedVUs})`,
    );
  }

  return {
    executor: 'ramping-arrival-rate',
    startRate,
    timeUnit: '1s',
    preAllocatedVUs,
    maxVUs,
    stages: [
      {
        duration: envString(`${prefix}_RAMP_UP`, defaults.rampUp),
        target: peakRate,
      },
      {
        duration: envString(`${prefix}_STEADY`, defaults.steady),
        target: peakRate,
      },
      {
        duration: envString(`${prefix}_RAMP_DOWN`, defaults.rampDown),
        target: 0,
      },
    ],
    gracefulStop: envString(`${prefix}_GRACEFUL_STOP`, '30s'),
  };
}

export function outcomeThresholds(prefix, scenarioName, defaults) {
  const maxErrorRate = envNumber(`${prefix}_MAX_ERROR_RATE`, defaults.maxErrorRate, 0);
  const p95Ms = envNumber(`${prefix}_P95_MS`, defaults.p95Ms, 1);
  const p99Ms = envNumber(`${prefix}_P99_MS`, defaults.p99Ms, 1);

  if (maxErrorRate >= 1) {
    throw new Error(`${prefix}_MAX_ERROR_RATE must be less than 1; received ${maxErrorRate}`);
  }

  return {
    [`http_req_duration{scenario:${scenarioName}}`]: [`p(95)<${p95Ms}`, `p(99)<${p99Ms}`],
    [`${prefix.toLowerCase()}_success_rate`]: [`rate>${1 - maxErrorRate}`],
    [`${prefix.toLowerCase()}_error_rate`]: [`rate<${maxErrorRate}`],
  };
}

export const SUMMARY_TREND_STATS = [
  'avg',
  'min',
  'p(50)',
  'p(90)',
  'p(95)',
  'p(99)',
  'max',
  'count',
];
