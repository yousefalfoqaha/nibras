FROM node:24-alpine AS client-build
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable
WORKDIR /build

RUN corepack enable

COPY /client .

ENV CI=true
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile
RUN pnpm run -r build

FROM eclipse-temurin:25-jdk AS api-build
WORKDIR /build

COPY --chmod=755 /api/mvnw ./
COPY /api/.mvn .mvn

COPY /api/pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY /api/src ./src
COPY --from=client-build /build/dist/ ./src/main/resources/static

RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app

RUN apt-get update && apt-get install -y curl caddy \
    && rm -rf /var/lib/apt/lists/*

COPY --from=api-build /build/target/*.jar ./app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["sh", "-c", "java -jar ./app.jar"]
