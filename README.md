# LogLens

## What is LogLens?
LogLens is a cloud-native, microservices-based backend that ingests log files, processes them asynchronously, and provides keyword + semantic search and AI-assisted analysis.

## Architecture Overview
The project follows a clean, layered architecture as defined in [ADR 0001](docs/adr/0001-layered-architecture.md):
- **API**: Handles incoming HTTP requests.
- **Service**: Contains core business logic.
- **Repository**: Manages data persistence using Spring Data JPA.

This structure is enforced by **ArchUnit** tests to ensure architectural compliance and maintain clear boundaries between components. Our persistence strategy, detailed in [ADR 0002](docs/adr/0002-jpa-strategy.md), defaults to lazy loading and uses optimistic locking to manage concurrency.

## Security Model
LogLens uses **JWT access tokens + opaque refresh tokens** for stateless authentication, as described in [ADR 0003](docs/adr/0003-auth-strategy.md).

- **Access tokens**: HMAC-SHA signed JWTs (1 h TTL) carrying user ID, email, and role
- **Refresh tokens**: Random UUIDs (30 d TTL) stored server-side in PostgreSQL
- **Password storage**: BCrypt (adaptive cost, default 10 rounds)
- **Authorization**: Role-based — `USER` and `ADMIN` roles enforced by Spring Security filter chain

| Endpoint | Access |
|---|---|
| `POST /v1/auth/register`, `/v1/auth/login`, `/v1/auth/refresh` | Public |
| `GET /health` | Public |
| `/v1/admin/**` | ADMIN only |
| Everything else | Authenticated |

For threat analysis and open issues, see [docs/security/threat-model.md](docs/security/threat-model.md).

## API Design
All endpoints are versioned under `/v1/` as defined in [ADR 0004](docs/adr/0004-versioning-strategy.md).

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/auth/register` | Register a new user |
| `POST` | `/v1/auth/login` | Authenticate and receive tokens |
| `POST` | `/v1/auth/refresh` | Refresh an expired access token |
| `POST` | `/v1/logs` | Ingest a structured log entry |
| `GET` | `/v1/logs` | List log entries (optional `?serviceName=` filter) |
| `GET` | `/health` | Health check (unversioned) |

The full API contract is documented in the [OpenAPI specification](OpenAPI.yaml). When running locally, the interactive Swagger UI is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

For our deprecation policy and version lifecycle, see [docs/api/deprecation-plan.md](docs/api/deprecation-plan.md).

## Features
- **Structured Log Ingestion**: `POST /v1/logs` accepts structured log entries (service name, level, message) and stores them in the `log_entries` table. `GET /v1/logs` retrieves entries with optional `?serviceName=` filtering.
- **File-based Log Ingestion**: Upload entire log files that are chunked and stored atomically via `LogService`.
- **N+1 Problem Demonstration & Solution**: The `LogService` includes both a naive (`getLogWithChunksNaive`) and an optimized (`getLogWithChunksOptimized`) method to demonstrate and solve the N+1 query problem.
- **Performance Testing**: Integration tests measure the exact number of SQL statements to verify the performance of different fetching strategies.

## Tech Stack
- Kotlin + Spring Boot
- **Persistence**: PostgreSQL with Spring Data JPA & Hibernate
- **Testing**: JUnit 5, Mockito, Testcontainers, ArchUnit
- (Planned) Kafka, Redis, AWS ECS/EKS, Terraform, pgvector, OpenTelemetry

## Local Development
### Requirements
- Docker

### Run Dependencies
The local environment, including the API, worker, and PostgreSQL database, is managed by Docker Compose. Health checks are configured to ensure services start in the correct order.
```bash
docker compose -f deploy/docker/docker-compose.local.yml up