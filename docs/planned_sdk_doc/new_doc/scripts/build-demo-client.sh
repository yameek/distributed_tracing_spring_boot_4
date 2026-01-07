#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLAYGROUND_DIR="${ROOT_DIR}/sdk-playground"

echo "=== Build SDK Playground ==="
echo "Root directory: ${ROOT_DIR}"
echo

run() {
  echo "+ $*"
  "$@"
  echo
}

if [[ ! -d "${PLAYGROUND_DIR}" ]]; then
  echo "sdk-playground directory not found at ${PLAYGROUND_DIR}" >&2
  exit 1
fi

run "${PLAYGROUND_DIR}/gradlew" -p "${PLAYGROUND_DIR}/demo-app" clean build
run "${PLAYGROUND_DIR}/gradlew" -p "${PLAYGROUND_DIR}/demo-client" clean build

echo "Playground server and client compiled successfully."
echo "Demo-generated SDKs live under sdk-playground/demo-app/build/generated/sources/annotationProcessor/..."

