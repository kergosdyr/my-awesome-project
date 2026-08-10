function safeLabel(value) {
  const sanitized = value.trim().replace(/[^0-9A-Za-z._-]+/g, '-').replace(/^-+|-+$/g, '');
  return sanitized || 'run';
}

function outputDirectory() {
  return (__ENV.RESULTS_DIR || 'load-tests/results').replace(/\/+$/, '');
}

function formatValue(value) {
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return String(value);
    }

    return Number.isInteger(value) ? String(value) : value.toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
  }

  return String(value);
}

function thresholdLines(metrics) {
  const lines = [];

  Object.keys(metrics)
    .sort()
    .forEach((metricName) => {
      const thresholds = metrics[metricName].thresholds || {};

      Object.keys(thresholds)
        .sort()
        .forEach((expression) => {
          lines.push(
            `- ${thresholds[expression].ok ? 'PASS' : 'FAIL'} ${metricName}: ${expression}`,
          );
        });
    });

  return lines.length > 0 ? lines : ['- No thresholds were reported by k6.'];
}

function metricLines(metrics) {
  return Object.keys(metrics)
    .sort()
    .map((metricName) => {
      const metric = metrics[metricName];
      const values = Object.keys(metric.values || {})
        .map((key) => `${key}=${formatValue(metric.values[key])}`)
        .join(', ');

      return `- ${metricName} (${metric.type}): ${values || 'no values reported'}`;
    });
}

function humanSummary(scenarioName, resultLabel, data) {
  const lines = [
    `k6 load-test summary: ${scenarioName}`,
    `result label: ${resultLabel}`,
    `generated at: ${new Date().toISOString()}`,
    '',
    'Thresholds',
    ...thresholdLines(data.metrics || {}),
    '',
    'Metrics',
    ...metricLines(data.metrics || {}),
    '',
    'The JSON file next to this report is the unmodified k6 handleSummary payload.',
    '',
  ];

  return lines.join('\n');
}

export function summaryOutputs(scenarioName, data) {
  const resultLabel = safeLabel(__ENV.RESULT_LABEL || scenarioName);
  const runDirectory = `${outputDirectory()}/${resultLabel}`;
  const text = humanSummary(scenarioName, resultLabel, data);

  return {
    [`${runDirectory}/summary.json`]: `${JSON.stringify(data, null, 2)}\n`,
    [`${runDirectory}/summary.txt`]: text,
    stdout: text,
  };
}
