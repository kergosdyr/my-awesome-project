#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

export BASE_URL="${BASE_URL:-http://localhost:8080}"
export RESULTS_DIR="${RESULTS_DIR:-$repo_root/load-tests/results}"
run_prefix="${LAB_RUN_PREFIX:-kafka-outbox-$(git rev-parse --short HEAD)}"
drain_timeout_seconds="${DRAIN_TIMEOUT_SECONDS:-30}"
overall_status=0

read_metric() {
  local json="$1"
  local field="$2"
  python3 -c 'import json,sys; print(json.loads(sys.argv[1])["data"][sys.argv[2]])' "$json" "$field"
}

wait_for_drain() {
  local deadline=$((SECONDS + drain_timeout_seconds))
  local metrics_json
  local pending
  local published
  local processed

  while (( SECONDS < deadline )); do
    metrics_json="$(curl --fail --silent --show-error "$BASE_URL/api/labs/events/metrics")"
    pending="$(read_metric "$metrics_json" pending)"
    published="$(read_metric "$metrics_json" published)"
    processed="$(read_metric "$metrics_json" processed)"
    if [[ "$pending" == "0" ]] && (( processed >= published )); then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for outbox/consumer drain after %s seconds.\n' "$drain_timeout_seconds" >&2
  return 1
}

run_strategy() {
  local strategy="$1"
  export EVENT_STRATEGY="$strategy"
  export RESULT_LABEL="${run_prefix}-${strategy}"

  bash load-tests/capture-environment.sh
  curl --fail --silent --show-error \
    --request POST \
    "$BASE_URL/api/labs/events/reset" \
    >"$RESULTS_DIR/$RESULT_LABEL/reset-response.json"

  local k6_status=0
  k6 run load-tests/scenarios/kafka-outbox-orders.js || k6_status=$?

  local drain_status=0
  wait_for_drain || drain_status=$?
  curl --fail --silent --show-error \
    "$BASE_URL/api/labs/events/metrics" \
    >"$RESULTS_DIR/$RESULT_LABEL/backend-metrics.json"

  if (( k6_status != 0 || drain_status != 0 )); then
    return 1
  fi
}

if ! run_strategy direct; then
  overall_status=1
fi
if ! run_strategy outbox; then
  overall_status=1
fi

exit "$overall_status"
