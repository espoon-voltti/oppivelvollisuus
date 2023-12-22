#!/usr/bin/env bash

set -euo pipefail

echo 'INFO: Waiting for compose stack to be up ...'
./wait-for-url.sh "http://frontend/health"
./wait-for-url.sh "http://api-gateway:3000/health"

echo "Running tests ..."

./gradlew e2eTest
