# EIU Plant Disease Detection System (EIU PDDS)

Quickstart guide for running the backend and infrastructure cleanly on any machine.

## Prerequisites

- Docker & Docker Compose v2

## Quick Start (Docker)

1. Create your local environment file from the template:

```bash
cp .env.example .env
```

> Replace `ROBOFLOW_API_KEY` in `.env` with a valid key if you want to run live image diagnosis (`POST /api/v1/diagnoses/analyze`). Default values for PostgreSQL, Redis, MinIO, and JWT already work out of the box for local testing.

2. Start the full stack:

```bash
# Production-like container build
docker compose -f infra/docker-compose.yml up -d --build

# Or Development mode with hot-reload (first run downloads Maven deps into maven_dev_cache)
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --build
```

3. Verify the backend is ready:
- Liveness probe: `http://localhost:8080/actuator/health/liveness` (`{"status":"UP"}`)
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running Tests

- Backend unit & integration tests:
  ```bash
  cd server && ./mvnw clean test
  ```
- Postman 28-request verification suite (requires an `admin_eiu` user with `ROLE_ADMIN` in PostgreSQL for Admin & Diagnosis folders):
  ```bash
  postman collection run postman/eiu_pdds_full_suite.postman_collection.json --env-var "baseUrl=http://localhost:8080"
  ```
