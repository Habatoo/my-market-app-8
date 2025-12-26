# syntax=docker/dockerfile:1
FROM gradle:9.1.0-jdk21 AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY bom/build.gradle bom/

COPY report/build.gradle* report/
COPY integration-tests/build.gradle* integration-tests/
COPY store/api/build.gradle* store/api/
COPY store/core/build.gradle* store/core/
COPY store/start/build.gradle* store/start/
COPY store/integrations/build.gradle* store/integrations/
COPY payment/api/build.gradle* payment/api/
COPY payment/start/build.gradle* payment/start/
COPY payment/integrations/build.gradle* payment/integrations/

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :store:start:dependencies --no-daemon

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :store:start:bootJar --no-daemon -x test

FROM amazoncorretto:21-alpine3.20-jdk
WORKDIR /app
COPY --from=build /app/store/start/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]