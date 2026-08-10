#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
base_url="${BASE_URL:-http://localhost:8080}"
base_url="${base_url%/}"
results_dir="${RESULTS_DIR:-$repo_root/docs/reports/raw/cache-stampede}"
product_id="${HOT_PRODUCT_ID:-1}"
run_count="${CACHE_LAB_RUNS:-3}"
short_sha="$(git -C "$repo_root" rev-parse --short HEAD)"
overall_status=0

cd "$repo_root"

for required_command in curl k6; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    printf '%s is required to run the cache-stampede lab.\n' "$required_command" >&2
    exit 1
  fi
done

if ! [[ "$run_count" =~ ^[1-9][0-9]*$ ]]; then
  printf 'CACHE_LAB_RUNS must be a positive integer; received %s.\n' "$run_count" >&2
  exit 1
fi

curl --fail --silent --show-error "$base_url/actuator/health" >/dev/null
curl --fail --silent --show-error "$base_url/api/products/$product_id" >/dev/null

for strategy in naive protected; do
  for run_number in $(seq 1 "$run_count"); do
    result_label="cache-stampede-${strategy}-run${run_number}-${short_sha}"
    run_dir="${results_dir%/}/$result_label"
    mkdir -p "$run_dir"

    curl --fail --silent --show-error \
      --request POST \
      "$base_url/api/labs/cache/reset" >"$run_dir/reset.json"
    printf '\n' >>"$run_dir/reset.json"

    BASE_URL="$base_url" \
      RESULTS_DIR="$results_dir" \
      RESULT_LABEL="$result_label" \
      bash "$script_dir/capture-environment.sh"

    set +e
    BASE_URL="$base_url" \
      RESULTS_DIR="$results_dir" \
      RESULT_LABEL="$result_label" \
      HOT_PRODUCT_ID="$product_id" \
      HOT_PRODUCT_PATH="/api/labs/cache/${strategy}/products/${product_id}" \
      k6 run "$script_dir/scenarios/hot-product-burst.js"
    k6_status=$?
    set -e

    curl --fail --silent --show-error \
      "$base_url/api/labs/cache/metrics" >"$run_dir/backend-metrics.json"
    printf '\n' >>"$run_dir/backend-metrics.json"
    curl --fail --silent --show-error \
      "$base_url/actuator/prometheus" >"$run_dir/backend-prometheus.txt"

    if [[ $k6_status -ne 0 ]]; then
      overall_status=$k6_status
      printf 'k6 thresholds failed for %s; metrics were still captured.\n' "$result_label" >&2
    fi
  done
done

exit "$overall_status"
