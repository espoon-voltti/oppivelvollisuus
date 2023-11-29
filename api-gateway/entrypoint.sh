#!/bin/bash

set -euo pipefail

if [ "${VOLTTI_ENV:-local}" != "local" ]; then
  s3download "$DEPLOYMENT_BUCKET" config /config
fi

exec "$@"
