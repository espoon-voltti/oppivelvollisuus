FROM eclipse-temurin:17-jammy as base

LABEL maintainer="https://github.com/espoon-voltti/oppivelvollisuus"

ENV LC_ALL C.UTF-8
ENV LANG C.UTF-8
ENV LANGUAGE C.UTF-8
RUN apt-get update \
 && apt-get -y dist-upgrade \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      tzdata \
      ca-certificates \
      curl \
      unzip \
 && ln -fs /usr/share/zoneinfo/Europe/Helsinki /etc/localtime \
 && dpkg-reconfigure --frontend noninteractive tzdata \
 && rm -rf /var/lib/apt/lists/*

FROM base as builder

WORKDIR /app

COPY ./gradle/ ./gradle/
COPY ./gradlew ./build.gradle.kts ./gradle.properties ./settings.gradle.kts ./

RUN ./gradlew --no-daemon resolveDependencies

COPY . .

# --offline is used to be sure that all dependencies are installed in previous steps
RUN ./gradlew --offline --no-daemon assemble \
 && unzip -oq build/libs/oppivelvollisuus-service-boot.jar -d target

FROM base

WORKDIR /app

COPY ./entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

COPY --from=builder /app/target/ .

ARG build=none
ARG commit=none
ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"
LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"
