FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

COPY pom.xml .

RUN ./mvnw -q -DskipTests dependency:resolve dependency:resolve-plugins

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]
