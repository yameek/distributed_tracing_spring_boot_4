#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

echo "=== Bits API SDK Generator bootstrap ==="
echo "Working directory: ${ROOT_DIR}"
echo

run() {
  echo "+ $*"
  "$@"
  echo
}

run ./gradlew --version
run ./gradlew clean
run ./gradlew :bits-sdk-annotations:build
run ./gradlew :bits-sdk-processor:build
run ./gradlew publishToMavenLocal

echo "Artifacts published to Maven Local (~/.m2/repository)."
echo "You can now run scripts/build-demo-client.sh or integrate the artifacts into your services."

