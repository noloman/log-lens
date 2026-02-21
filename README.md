# LogLens

## What is LogLens?
LogLens is a cloud-native, microservices-based backend that ingests log files, processes them asynchronously, and provides keyword + semantic search and AI-assisted analysis.

## Architecture Overview
**Week 1 (v1):**
API → Postgres  
Worker → (bootstrapped only)

## Tech Stack
- Kotlin + Spring Boot
- PostgreSQL (Docker)
- (Planned) Kafka, Redis, AWS ECS/EKS, Terraform, pgvector, OpenTelemetry

## Local Development
### Requirements
- Docker

### Run dependencies
```bash
docker compose -f deploy/docker/docker-compose.local.yml up