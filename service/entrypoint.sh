#!/usr/bin/env bash

set -euo pipefail

# For log tagging (with a default value and error logging without crashing)
# shellcheck disable=SC2155
export HOST_IP=$(curl --silent --fail --show-error http://169.254.169.254/latest/meta-data/local-ipv4 || printf 'UNAVAILABLE')

# shellcheck disable=SC2086
exec java -cp . -server $JAVA_OPTS org.springframework.boot.loader.JarLauncher "$@"
