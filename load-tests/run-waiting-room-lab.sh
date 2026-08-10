#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
base_url="${BASE_URL:-http://localhost:8080}"
base_url="${base_url%/}"
results_dir="${RESULTS_DIR:-$repo_root/docs/reports/raw/waiting-room}"
run_count="${WAITING_ROOM_RUNS:-3}"
short_sha="$(git -C "$repo_root" rev-parse --short HEAD)"
overall_status=0

cd "$repo_root"

for required_command in curl k6; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    printf '%s is required to run the waiting-room lab.\n' "$required_command" >&2
    exit 1
  fi
done

if ! [[ "$run_count" =~ ^[1-9][0-9]*$ ]]; then
  printf 'WAITING_ROOM_RUNS must be a positive integer; received %s.\n' "$run_count" >&2
  exit 1
fi

curl --fail --silent --show-error "$base_url/actuator/health" >/dev/null

run_mode() {
  local mode="$1"
  local run_number="$2"
  local result_label="waiting-room-${mode}-run${run_number}-${short_sha}"
  local run_dir="${results_dir%/}/$result_label"
  local k6_status=0

  mkdir -p "$run_dir"
  curl --fail --silent --show-error \
    --request POST \
    "$base_url/api/labs/waiting-room/reset" >"$run_dir/reset.json"
  printf '\n' >>"$run_dir/reset.json"

  BASE_URL="$base_url" \
    RESULTS_DIR="$results_dir" \
    RESULT_LABEL="$result_label" \
    bash "$script_dir/capture-environment.sh"

  BASE_URL="$base_url" \
    RESULTS_DIR="$results_dir" \
    RESULT_LABEL="$result_label" \
    WAITING_ROOM_MODE="$mode" \
    k6 run "$script_dir/scenarios/flash-sale-admission.js" || k6_status=$?

  curl --fail --silent --show-error \
    "$base_url/api/labs/waiting-room/metrics" >"$run_dir/backend-metrics.json"
  printf '\n' >>"$run_dir/backend-metrics.json"
  curl --fail --silent --show-error \
    "$base_url/actuator/prometheus" >"$run_dir/backend-prometheus.txt"

  if (( k6_status != 0 )); then
    overall_status=$k6_status
    printf 'k6 thresholds failed for %s; backend evidence was still captured.\n' "$result_label" >&2
  fi
}

for run_number in $(seq 1 "$run_count"); do
  if (( run_number % 2 == 1 )); then
    run_mode direct "$run_number"
    run_mode queued "$run_number"
  else
    run_mode queued "$run_number"
    run_mode direct "$run_number"
  fi
done

exit "$overall_status"
