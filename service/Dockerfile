# SPDX-FileCopyrightText: 2023-2024 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

FROM eclipse-temurin:25-jammy AS base

ARG CACHE_BUST=2025-W52

LABEL maintainer="https://github.com/espoon-voltti/oppivelvollisuus"

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8

ARG S3_DOWNLOADER_VERSION=v1.5.1
ARG S3_DOWNLOADER_SHA256="cb751589ca60749c42b65009671d6076435e7454b8980ee98884e296a1ec6cb8"

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
 && curl -sSfL https://github.com/espoon-voltti/s3-downloader/releases/download/${S3_DOWNLOADER_VERSION}/s3downloader-linux-amd64 \
       -o /bin/s3download \
 && echo "${S3_DOWNLOADER_SHA256}  /bin/s3download" | sha256sum -c - \
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

ENV USERNAME=oppivelvollisuus
ENV HOME_DIR=/home/${USERNAME}
ENV USER_ID=1000
ENV GROUP_ID=1000

RUN addgroup --gid ${GROUP_ID} ${USERNAME} \
 && adduser --uid ${USER_ID} --gid ${GROUP_ID} --gecos "" -q --home ${HOME_DIR} --disabled-password ${USERNAME} \
 && mkdir -p /config \
 && chown ${USERNAME}:${USERNAME} /config

VOLUME /config
VOLUME /tmp

COPY --from=builder /app/target/ .

ARG build=none
ARG commit=none
ENV APP_BUILD="$build"
ENV APP_COMMIT="$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

USER ${USERNAME}
