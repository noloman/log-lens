# LogLens

## What is LogLens?
LogLens is a cloud-native, microservices-based backend that ingests log files, processes them asynchronously, and provides keyword + semantic search and AI-assisted analysis.

## Architecture Overview
The project follows a clean, layered architecture as defined in [ADR 0001](docs/adr/0001-layered-architecture.md):
- **API**: Handles incoming HTTP requests.
- **Service**: Contains core business logic.
- **Repository**: Manages data persistence using Spring Data JPA.

This structure is enforced by **ArchUnit** tests to ensure architectural compliance and maintain clear boundaries between components. Our persistence strategy, detailed in [ADR 0002](docs/adr/0002-jpa-strategy.md), defaults to lazy loading and uses optimistic locking to manage concurrency.

## Features
- **Transactional Log Ingestion**: Create logs and their associated chunks in a single, atomic transaction.
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