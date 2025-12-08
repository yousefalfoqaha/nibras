FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY --chmod=0755 mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

COPY pom.xml .

RUN ./mvnw -q -DskipTests dependency:resolve dependency:resolve-plugins
