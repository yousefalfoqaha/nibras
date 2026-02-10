FROM node:20-alpine AS client-build
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable
WORKDIR /build

RUN corepack enable

COPY /client .

RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --prod --frozen-lockfile
RUN pnpm run build

FROM eclipse-temurin:25-jdk AS api-build
WORKDIR /build

COPY --chmod=755 /api/mvnw ./
COPY /api/.mvn .mvn

COPY /api/pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY /api/src ./src
COPY --from=client-build /build/client/dist/ ./src/main/resources/client

RUN ./mvnw clean package -DskipTests -B

FROM alpine AS runtime
WORKDIR /app

RUN apk add --no-cache openjdk25-jre nodejs caddy curl gosu

COPY --from=api-build /build/api/target/*.jar ./app.jar

COPY /scripts/docker/entrypoint.sh .
RUN chmod +x ./entrypoint.sh

COPY /reverse-proxy/ ./reverse-proxy/

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["/bin/sh", "-c", "/app/entrypoint.sh"]
