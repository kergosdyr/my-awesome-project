# Load report source notes

This file is supporting material for the generated technical report. It is not the reader-facing report.

## Reporting job

- Question: Under controlled local commerce workloads, how do stampede protection, a Kafka transactional outbox, and a Redis waiting room change user-visible latency/throughput, mechanism-specific load, and correctness risk relative to their within-lab baselines?
- Audience: technical.
- Scope: local single-host runs captured on 2026-08-11 KST; three repetitions per measured group where the lab can run reliably.
- Comparison basis: only strategies run with the same lab code, infrastructure, fixture, warm-up, arrival-rate profile, and success definition are treated as direct before/after comparisons.
- Decision use: identify what each mechanism actually protects, the latency/complexity it adds, and what remains unproven before production use.
- Success criteria: raw k6 summaries and environment captures exist; headline medians are independently recomputed; correctness counters and known missing metrics are visible; causal wording is limited to controlled changes.

## Required report structure mapping

1. Title → `Commerce Labs: Load-Test Results`
2. Technical summary → answer-first summary of the three controlled comparisons.
3. Key findings with visual evidence → one section per lab, with an adjacent interpretation paragraph for every chart.
4. Scope, data, and metric definitions → requests, successful requests, end-to-end wait, origin load, outbox drain, FIFO/capacity definitions.
5. Methodology → hardware/container limits, fixture, warm-up, arrival-rate stages, repetitions, median rule.
6. Limitations and robustness → local single node, short runs, controlled delays, missing time series, failed or partial failure injection.
7. Recommended next steps → longer soak, multi-instance, resource telemetry, failure injection.
8. Further questions → production-scale assumptions that remain open.

## Source inventory

- `docs/reports/raw/cache-stampede/**/summary.json`: unmodified k6 handleSummary output.
- `docs/reports/raw/cache-stampede/**/environment.txt`: Git/runtime/hardware/container context.
- `docs/reports/raw/cache-stampede/**/backend-metrics.json|txt`: HTTP, cache, origin, and lock counters.
- `docs/reports/raw/kafka-outbox/**`: direct/outbox k6 output, relay/consumer counters, environment.
- `docs/reports/raw/waiting-room/**`: direct/queued flow output, capacity/fairness counters, environment.
- `load-tests/scenarios/**` and branch-specific runners: executable workload definitions.
- `docs/labs/*.md`: transaction, failure-policy, and interpretation notes.

Missing sources must remain missing; no values are to be inferred from neighboring runs.

## Metric definitions

- HTTP p50/p95/p99: k6 `http_req_duration` percentiles for the named scenario and measured run.
- Successful throughput: scenario-specific successful iteration counter divided by measured wall-clock duration, not raw target RPS.
- Error rate: requests failing the scenario's HTTP and response-contract checks divided by scenario attempts.
- Group headline: median of three independently captured run-level values. Raw requests are not pooled because that would overweight longer runs.
- Change: `(experiment median - baseline median) / baseline median × 100`; percentage-point differences are used for rates when clearer.
- Origin loads: actual slow-origin executions reported by the cache lab, not inferred misses.
- Outbox drain: elapsed time until pending outbox reaches zero after request generation stops.
- FIFO violation: an admitted ticket whose sequence is greater than a still-waiting eligible ticket, per waiting-room backend counters.

## Chart map

| Report segment | Question | Family / type | Fields | Supported claim | Palette |
| --- | --- | --- | --- | --- | --- |
| Cache stampede | How did protection change tail latency? | Comparison / bar | strategy, p95_ms | Protected and naive latency under the same slow-origin workload | single blue root + neutral |
| Cache stampede | How many origin loads reached MySQL? | Comparison / bar | strategy, origin_loads | Locking changes amplification at expiry | single navy root + neutral |
| Kafka/outbox | What request latency distribution was observed? | Comparison / grouped bar | strategy, percentile, latency_ms | Observed API latency under the fixed low-load execution order; not a general speed claim | single blue root + neutral |
| Waiting room | Did overload become queueing rather than failure? | Composition / stacked bar | strategy, outcome, share | The queue shifts outcome mix; it does not make capacity free | blue/olive/neutral, max three roots |
| Waiting room | What wait did admitted users pay? | Comparison / bar | strategy, end_to_end_p95_ms | Protection trades immediate failures for bounded wait | single blue root + neutral |

Every chart will have an adjacent narrative block, explicit unit/sample context, a zero baseline for absolute bars, and a canonical source reference.

## Validation notes

- Recompute each headline from the three raw JSON files independently of the lab report table.
- Reconcile attempt, success, queued/rejected, and error counts where categories should sum.
- Confirm equal workload environment variables and commit identity inside each comparison.
- Treat a dropped iteration as load-generator saturation and exclude or rerun the affected comparison.
- Treat very favorable results as local evidence only; retain max values and counterexamples.
- Keep the interrupted waiting-room preflights as failure evidence, but exclude them from before/after calculations. Their global `http_reqs` counts are not endpoint-specific poll counts.
- Waiting-room direct and queued use the same open-model arrival profile but different client VU pools (`200/1,000` versus `3,000/4,000`). Report this as a client-side comparison caveat.
- Direct waiting-room 429s are built-in k6 HTTP failures but controlled outcomes in the custom scenario check. Never summarize this as “HTTP errors 0”; separate server/contract errors from business rejection.
- Waiting-room HTTP percentile thresholds cover individual ticket/poll/purchase calls, not the user-flow E2E wait. Worst final wait was 28.272 seconds, only 1.728 seconds below ticket TTL.
- Prometheus waiting-room snapshots are process-cumulative across earlier pilots and runs. Use each k6 summary for run-level HTTP counts and the resettable lab metrics endpoint for run-level correctness counters.
- Label Kafka order 584's direct-failure attribution as an inference: the DB state and experiment order strongly support it, but the failed HTTP response did not contain a correlation ID.
- Describe 14.576 seconds as outbox occurrence-to-publish recovery time, not consumer end-to-end latency. The final `publishFailures=3` combines one direct failure and two relay failures.
