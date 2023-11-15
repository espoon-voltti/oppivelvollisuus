# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM node:20.9-bookworm-slim AS base

WORKDIR /app

FROM base AS builder

COPY ./package.json ./yarn.lock ./

RUN yarn install --immutable

COPY . .

RUN yarn build

FROM builder AS test

RUN yarn lint
#RUN yarn test

FROM builder

ENV NODE_ENV production

ARG build=none
ARG commit=none

ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"

LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

CMD ["node", "dist/index.js"]
