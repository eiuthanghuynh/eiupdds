#!/usr/bin/env bash
# ==============================================================================
# Docker Compose Runtime Smoke Test Script for EIU PDDS
# Validates compose syntax, builds images, boots the full container stack,
# polls for container health status, probes actuator HTTP, and tears down cleanly.
# ==============================================================================
set -euo pipefail

CLEANUP_CALLED=0
cleanup() {
    if [ "$CLEANUP_CALLED" -eq 0 ]; then
        CLEANUP_CALLED=1
        echo "Tearing down Docker Compose services..."
        docker compose -f infra/docker-compose.yml down -v 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

echo "=== [1/5] Preparing Test Environment Variables ==="
# Ensure .env exists with non-empty compliant credentials
cat << 'EOF' > .env
ROBOFLOW_API_KEY=ci_smoke_test_key_for_testing_purposes
POSTGRES_DB=eiu_pdds_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
POSTGRES_PORT=5432
REDIS_PORT=6379
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_BUCKET_NAME=eiu-pdds-plants
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080
JWT_SECRET=ci-long-term-hardened-jwt-secret-key-with-minimum-256-bits-length-for-hmac
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
GOOGLE_CLIENT_ID=ci-google-client-id-for-smoke-test.apps.googleusercontent.com
EOF

echo "=== [2/5] Validating Docker Compose Configuration Syntax ==="
docker compose -f infra/docker-compose.yml config --quiet
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml config --quiet
echo "Docker Compose configurations are syntactically valid."

echo "=== [3/5] Verifying Development Dockerfile Build ==="
docker build -f server/Dockerfile.dev -t eiu-pdds-server:dev-smoke server/
echo "Development Dockerfile builds successfully."

echo "=== [4/5] Launching Production Docker Compose Stack ==="
docker compose -f infra/docker-compose.yml up -d --build

echo "=== [5/5] Polling Backend Container Health (Timeout: 120s) ==="
MAX_ATTEMPTS=24
SLEEP_INTERVAL=5
HEALTHY=0

for ((i=1; i<=MAX_ATTEMPTS; i++)); do
    STATUS=$(docker inspect --format='{{json .State.Health.Status}}' eiu-pdds-backend 2>/dev/null || echo '"starting"')
    echo "[$((i * SLEEP_INTERVAL))s / $((MAX_ATTEMPTS * SLEEP_INTERVAL))s] Backend container health status: $STATUS"
    
    if [ "$STATUS" = '"healthy"' ]; then
        HEALTHY=1
        break
    fi
    sleep "$SLEEP_INTERVAL"
done

if [ "$HEALTHY" -ne 1 ]; then
    echo "::error::Backend container failed to achieve 'healthy' status within $((MAX_ATTEMPTS * SLEEP_INTERVAL))s!"
    echo "Recent container logs:"
    docker logs eiu-pdds-backend --tail 120
    exit 1
fi

echo "Backend is healthy! Verifying Spring Boot Actuator Liveness probe..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health/liveness || echo "000")
if [ "$HTTP_CODE" != "200" ]; then
    echo "::error::Actuator liveness probe returned HTTP status $HTTP_CODE (expected 200)"
    docker logs eiu-pdds-backend --tail 50
    exit 1
fi

echo "Actuator probe succeeded (HTTP 200)."
echo "=== Docker Compose Smoke Test completed successfully! ==="
