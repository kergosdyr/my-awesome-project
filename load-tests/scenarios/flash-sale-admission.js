import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Gauge, Trend } from 'k6/metrics';

import {
  arrivalRateScenario,
  envString,
  outcomeThresholds,
  SUMMARY_TREND_STATS,
  urlFor,
} from '../lib/config.js';
import { getJson, postJson, tryJson } from '../lib/http.js';
import { createOutcomeMetrics, recordOutcome } from '../lib/metrics.js';
import { summaryOutputs } from '../lib/summary.js';

const mode = envString('WAITING_ROOM_MODE', 'direct').toLowerCase();
const SCENARIO = `waiting_room_${mode}`;
const BASE_PATH = '/api/labs/waiting-room';
const metrics = createOutcomeMetrics('waiting_room');
const acceptedTotal = new Counter('waiting_room_accepted_total');
const queuedTotal = new Counter('waiting_room_queued_total');
const rejectedTotal = new Counter('waiting_room_rejected_total');
const waitDuration = new Trend('waiting_room_wait_duration', true);
const backendMaxActive = new Gauge('waiting_room_backend_max_active');
const backendMaxQueueDepth = new Gauge('waiting_room_backend_max_queue_depth');
const backendFifoViolations = new Gauge('waiting_room_backend_fifo_violations');

if (!['direct', 'queued'].includes(mode)) {
  throw new Error(`WAITING_ROOM_MODE must be direct or queued; received "${mode}"`);
}

export const options = {
  scenarios: {
    [SCENARIO]: {
      ...arrivalRateScenario('WAITING_ROOM', {
        startRate: 10,
        peakRate: 100,
        rampUp: '15s',
        steady: '1m',
        rampDown: '15s',
        preAllocatedVUs: 1000,
        maxVUs: 4000,
      }),
      exec: 'compareWaitingRoom',
      tags: { lab: 'waiting-room', mode },
    },
  },
  thresholds: outcomeThresholds('WAITING_ROOM', SCENARIO, {
    maxErrorRate: 0.01,
    p95Ms: 1000,
    p99Ms: 2000,
  }),
  summaryTrendStats: SUMMARY_TREND_STATS,
};

function recordBackendMetrics() {
  const response = getJson(urlFor(`${BASE_PATH}/metrics`), {
    name: 'GET waiting-room metrics',
    operation: 'waiting-room-metrics',
    mode,
  });
  const body = tryJson(response);
  if (response.status !== 200 || !body?.data) return;

  const maxActive = Math.max(
    body.data.downstream?.maxActive || 0,
    body.data.waitingRoom?.maxActive || 0,
  );
  backendMaxActive.add(maxActive, { mode });
  backendMaxQueueDepth.add(body.data.waitingRoom?.maxQueueDepth || 0, { mode });
  backendFifoViolations.add(body.data.waitingRoom?.fifoViolations || 0, { mode });
}

function sampleBackendMetrics() {
  if (exec.scenario.iterationInTest % 20 === 0) {
    recordBackendMetrics();
  }
}

function directPurchase() {
  const response = postJson(
    urlFor(`${BASE_PATH}/direct`),
    {},
    { name: 'POST direct flash-sale purchase', operation: 'direct-purchase', mode },
  );
  const accepted = response.status === 200;
  const rejected = response.status === 429;

  if (accepted) acceptedTotal.add(1, { mode });
  if (rejected) rejectedTotal.add(1, { mode });

  const expected = check(response, {
    'direct returns accepted or controlled rejection': () => accepted || rejected,
    'direct never returns a server error': (result) => result.status < 500,
  });
  recordOutcome(metrics, expected, { mode, operation: 'direct-purchase' });
  sampleBackendMetrics();
}

function queuedPurchase() {
  const startedAt = Date.now();
  const issueResponse = postJson(
    urlFor(`${BASE_PATH}/tickets`),
    {},
    { name: 'POST waiting-room ticket', operation: 'issue-ticket', mode },
  );
  const issued = tryJson(issueResponse)?.data;
  if (issueResponse.status !== 202 || !issued?.ticketId) {
    rejectedTotal.add(1, { mode, stage: 'ticket' });
    recordOutcome(metrics, false, { mode, operation: 'queued-flow' });
    sampleBackendMetrics();
    return;
  }
  queuedTotal.add(1, { mode });

  const deadline = Date.parse(issued.expiresAt);
  let admission = null;
  while (Date.now() < deadline) {
    sleep(Math.max(issued.pollAfterMillis || 100, 10) / 1000);
    const pollResponse = getJson(
      urlFor(`${BASE_PATH}/tickets/${issued.ticketId}`),
      { name: 'GET waiting-room ticket', operation: 'poll-ticket', mode },
    );
    const polled = tryJson(pollResponse)?.data;
    if (pollResponse.status === 202 && polled?.status === 'QUEUED') {
      continue;
    }
    if (pollResponse.status === 200 && polled?.status === 'ADMITTED') {
      admission = polled;
    }
    break;
  }

  if (!admission?.admissionToken) {
    rejectedTotal.add(1, { mode, stage: 'admission' });
    recordOutcome(metrics, false, { mode, operation: 'queued-flow' });
    sampleBackendMetrics();
    return;
  }
  waitDuration.add(admission.waitDurationMillis ?? Date.now() - startedAt, { mode });

  const purchaseResponse = postJson(
    urlFor(`${BASE_PATH}/purchase`),
    {
      ticketId: issued.ticketId,
      admissionToken: admission.admissionToken,
    },
    { name: 'POST admitted flash-sale purchase', operation: 'queued-purchase', mode },
  );
  const accepted = purchaseResponse.status === 200;
  if (accepted) {
    acceptedTotal.add(1, { mode });
  } else {
    rejectedTotal.add(1, { mode, stage: 'purchase' });
  }

  const successful = check(purchaseResponse, {
    'admitted purchase is accepted': () => accepted,
    'queued flow never returns a server error': (result) => result.status < 500,
  });
  recordOutcome(metrics, successful, { mode, operation: 'queued-flow' });
  sampleBackendMetrics();
}

export function compareWaitingRoom() {
  if (mode === 'direct') {
    directPurchase();
    return;
  }
  queuedPurchase();
}

export function handleSummary(data) {
  return summaryOutputs(`waiting-room-${mode}`, data);
}
