#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
NUM_CLIENTS="${NUM_CLIENTS:-50}"

command -v k6 >/dev/null 2>&1 || {
  echo "k6 is not installed. See https://k6.io/docs/get-started/installation/" >&2
  exit 1
}

echo "Target: ${BASE_URL}"
echo "Synthetic clients: ${NUM_CLIENTS}"
echo

echo "-- current file descriptor limit (client side): $(ulimit -n)"
echo "   raise it if you see 'too many open files': ulimit -n 65535"
echo

k6 run \
  -e BASE_URL="${BASE_URL}" \
  -e NUM_CLIENTS="${NUM_CLIENTS}" \
  "$(dirname "$0")/k6-script.js"
