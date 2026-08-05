# syntax=docker/dockerfile:1.7

FROM node:24.19.0-alpine3.23 AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21.0.12_8-jdk-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/ ./
COPY --from=frontend-build /workspace/frontend/dist/ ./src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw \
    && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21.0.12_8-jre-alpine AS runtime
RUN addgroup -S walletwise \
    && adduser -S -G walletwise -h /app walletwise
WORKDIR /app
COPY --from=backend-build --chown=walletwise:walletwise /workspace/backend/target/*.jar /app/walletwise.jar
USER walletwise
ENV PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q -O - "http://127.0.0.1:${PORT}/actuator/health" >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/walletwise.jar"]
