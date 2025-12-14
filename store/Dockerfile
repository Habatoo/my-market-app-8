FROM gradle:9.1.0-jdk21 AS build

USER root
RUN apt-get update && apt-get install -y ca-certificates openssl && update-ca-certificates

WORKDIR /app

COPY . /app
COPY env/application.yaml /app/application.yaml
COPY env/.env /app/.env

RUN chmod +x ./gradlew
RUN ./gradlew :store:start:bootJar --no-daemon -x test

FROM amazoncorretto:21-alpine3.20-jdk
WORKDIR /app

COPY --from=build /app/store/start/build/libs/start-*.jar app.jar
COPY --from=build /app/application.yaml /app/application.yaml
COPY --from=build /app/.env /app/.env

ENV SPRING_CONFIG_LOCATION=/app/application.yaml
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
