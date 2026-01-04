FROM maven:3.9-eclipse-temurin-25-alpine
WORKDIR /app

COPY pom.xml ./

RUN mvn -q -DskipTests dependency:resolve

EXPOSE 8080

