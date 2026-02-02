#!/bin/sh -eu

# SPDX-FileCopyrightText: 2023-2024 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

# shellcheck disable=SC2155

set -eu

if [ "${DEBUG:-false}" = "true" ]; then
  set -x
fi

export HOST_IP="UNAVAILABLE"

if [ "${API_GATEWAY_URL:-X}" = 'X' ]; then
  echo 'ERROR: API_GATEWAY_URL must be a non-empty string!'
  exit 1
fi

cp -r /etc/nginx/ /tmp/nginx/

for directory in /tmp/nginx/conf.d/ /tmp/nginx/; do
  gomplate --input-dir="$directory" --output-map="$directory"'{{ .in | strings.ReplaceAll ".template" "" }}'
done

if [ "${DEBUG:-false}" = "true" ]; then
  cat /tmp/nginx/nginx.conf
  cat /tmp/nginx/conf.d/default.conf
fi

if [ "${BASIC_AUTH_ENABLED:-false}" = 'true' ]; then
  echo "$BASIC_AUTH_CREDENTIALS" > /tmp/nginx/.htpasswd
fi

exec "$@"
