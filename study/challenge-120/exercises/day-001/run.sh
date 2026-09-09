#!/usr/bin/env bash
set -euo pipefail
exercise_dir="$(cd "$(dirname "$0")" && pwd)"
repo_dir="$(cd "$exercise_dir/../../../.." && pwd)"
case "${1:-all}" in
  coding) shift; exec "$repo_dir/backend/gradlew" -p "$exercise_dir" test --tests challenge.LongestBatchTest "$@" ;;
  backend) shift; exec "$repo_dir/backend/gradlew" -p "$exercise_dir" test --tests challenge.ReservationServiceTest "$@" ;;
  all) if [ "$#" -gt 0 ]; then shift; fi; exec "$repo_dir/backend/gradlew" -p "$exercise_dir" test "$@" ;;
  compile) shift; exec "$repo_dir/backend/gradlew" -p "$exercise_dir" testClasses "$@" ;;
  *) echo "Usage: bash run.sh [coding|backend|all|compile] [Gradle options]" >&2; exit 2 ;;
esac
