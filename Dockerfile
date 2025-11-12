FROM gradle:9.1.0-jdk21 AS build

USER root
RUN apt-get update && apt-get install -y ca-certificates openssl && update-ca-certificates

WORKDIR /app

COPY . .
COPY ./env/application.yaml /app/application.yaml
COPY ./env/.env /app/.env

ENV SPRING_CONFIG_LOCATION=/app/application.yaml
ENV SPRING_PROFILES_ACTIVE=dev

RUN ./gradlew clean :start:bootJar --no-daemon --stacktrace

FROM openjdk:21-jdk-slim

WORKDIR /app
COPY --from=build /app/start/build/libs/start-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/application.yaml /app/application.yaml
COPY --from=build /app/.env /app/.env

EXPOSE 8080

ENV SPRING_CONFIG_LOCATION=/app/application.yaml
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
