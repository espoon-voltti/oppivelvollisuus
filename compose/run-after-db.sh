#!/bin/bash

# SPDX-FileCopyrightText: 2023-2026 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

set -euo pipefail

port=5432
until nc -z localhost "$port"; do
  echo "Waiting for port $port"
  sleep 1
done

exec "$@"
