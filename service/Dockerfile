# SPDX-FileCopyrightText: 2023-2024 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM eclipse-temurin:21-jammy AS base

ARG CACHE_BUST=2025-W07

LABEL maintainer="https://github.com/espoon-voltti/oppivelvollisuus"

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
      unzip \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/v1.4.1/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && chmod +x /bin/s3download

FROM base AS builder

WORKDIR /app

COPY ./gradle/ ./gradle/
COPY ./gradlew ./build.gradle.kts ./gradle.properties ./settings.gradle.kts ./

RUN ./gradlew --no-daemon resolveDependencies

COPY . .


# --offline is used to be sure that all dependencies are installed in previous steps
RUN ./gradlew --offline --no-daemon ktlintCheck
RUN ./gradlew --offline --no-daemon assemble  \
 && unzip -oq build/libs/oppivelvollisuus-service-boot.jar -d target


FROM base

WORKDIR /app

COPY ./entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

COPY --from=builder /app/target/ .

ARG build=none
ARG commit=none
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
