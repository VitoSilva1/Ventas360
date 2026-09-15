FROM maven:3.9-eclipse-temurin-21 AS build

ARG SERVICE_DIR
WORKDIR /workspace

COPY backend/${SERVICE_DIR}/pom.xml backend/${SERVICE_DIR}/pom.xml
COPY backend/${SERVICE_DIR}/src backend/${SERVICE_DIR}/src

WORKDIR /workspace/backend/${SERVICE_DIR}
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

ARG SERVICE_DIR
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/backend/${SERVICE_DIR}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
