#!/usr/bin/env bash

set -euo pipefail

results_dir="${RESULTS_DIR:-load-tests/results}"
result_label="${RESULT_LABEL:-run}"
safe_label="$(printf '%s' "$result_label" | LC_ALL=C sed -E -e 's/[^0-9A-Za-z._-]+/-/g' -e 's/^-*//' -e 's/-*$//')"

if [[ -z "$safe_label" ]]; then
  safe_label="run"
fi

run_dir="${results_dir%/}/$safe_label"
mkdir -p "$run_dir"

command_output() {
  local command_name="$1"
  shift

  if command -v "$command_name" >/dev/null 2>&1; then
    "$command_name" "$@" 2>&1 || true
  else
    printf '%s\n' "$command_name: not installed"
  fi
}

{
  printf 'captured_at_utc: %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  printf 'git_commit: %s\n' "$(git rev-parse HEAD 2>/dev/null || printf 'not-a-git-worktree')"
  printf 'git_describe: %s\n' "$(git describe --always --dirty 2>/dev/null || printf 'not-a-git-worktree')"
  printf 'base_url: %s\n' "${BASE_URL:-http://localhost:8080}"
  printf 'result_label: %s\n' "$safe_label"
  printf '\n[operating-system]\n'
  command_output uname -a
  command_output sw_vers
  printf '\n[cpu-and-memory]\n'
  command_output sysctl -n machdep.cpu.brand_string
  command_output sysctl -n hw.ncpu
  command_output sysctl -n hw.memsize
  printf '\n[k6]\n'
  command_output k6 version
  printf '\n[java]\n'
  command_output java -version
  printf '\n[docker]\n'
  command_output docker version
  printf '\n[compose-services]\n'
  command_output docker compose ps
} >"$run_dir/environment.txt"

printf 'Environment captured at %s\n' "$run_dir/environment.txt"
