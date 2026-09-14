#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
case "${1:-all}" in
  coding) shift; exec "$project_dir/gradlew" -p "$project_dir" test --tests challenge.coding.LongestBatchTest "$@" ;;
  backend) shift; exec "$project_dir/gradlew" -p "$project_dir" legacyTest --tests challenge.reservation.basic.ReservationServiceTest "$@" ;;
  all) if [ "$#" -gt 0 ]; then shift; fi; exec "$project_dir/gradlew" -p "$project_dir" test --tests challenge.coding.LongestBatchTest legacyTest --tests "challenge.reservation.basic.*" "$@" ;;
  compile) shift; exec "$project_dir/gradlew" -p "$project_dir" testClasses legacyTestsClasses "$@" ;;
  *) echo "Usage: bash run.sh [coding|backend|all|compile] [Gradle options]" >&2; exit 2 ;;
esac
