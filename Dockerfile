FROM gradle:9.1.0-jdk21 AS build

USER root
RUN apt-get update && apt-get install -y ca-certificates openssl && update-ca-certificates

WORKDIR /app

COPY settings.gradle /app/settings.gradle
COPY build.gradle /app/build.gradle

COPY api/build.gradle /app/api/build.gradle
COPY api /app/api
COPY bom/build.gradle /app/bom/build.gradle
COPY bom /app/bom
COPY core/build.gradle /app/core/build.gradle
COPY core /app/core
COPY integrations/build.gradle /app/integrations/build.gradle
COPY integrations /app/integrations
COPY report/build.gradle /app/report/build.gradle
COPY report /app/report
COPY start/build.gradle /app/start/build.gradle
COPY start /app/start

RUN gradle --no-daemon dependencies

COPY . .
COPY ./env/application.yaml /app/application.yaml
COPY ./env/.env /app/.env

ENV SPRING_CONFIG_LOCATION=/app/application.yaml
ENV SPRING_PROFILES_ACTIVE=dev

RUN ./gradlew clean :start:bootJar --no-daemon --stacktrace

FROM amazoncorretto:21-alpine3.20-jdk
WORKDIR /app
COPY --from=build /app/start/build/libs/start-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/application.yaml /app/application.yaml
COPY --from=build /app/.env /app/.env

EXPOSE 8080

ENV SPRING_CONFIG_LOCATION=/app/application.yaml
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
