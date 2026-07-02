# LogLens Backend Engineering Roadmap

LogLens is a backend engineering project with a product goal: build a small but credible backend system for uploading, processing, storing, and analyzing application logs. The project is intentionally scoped like a compact Datadog, Sentry, or Logtail backend, with production-shaped implementation, tests, documentation, and operational concerns.

Use this document as the working checklist. Keep items checked only when the code, tests, and documentation are all in place.

For the project engineering method, see [`docs/engineering/backend-engineering-guide.md`](../engineering/backend-engineering-guide.md).

## Current Project Status

Last reviewed: 2026-07-02.

Overall status: **Phase 1 in progress**. The repository has a working Kotlin/Spring multi-module scaffold with API and worker apps, Dockerfiles, local Compose for API/worker/Postgres/Redis, JWT auth, structured log entry ingestion, JPA entities/repositories, idempotency support, Redis cache wiring, per-IP rate limiting, OpenAPI, ADRs, and a threat model.

Current health:

- [x] Multi-module Gradle project compiles.
- [x] Unit and MVC/controller tests are largely passing.
- [x] API and worker health endpoints exist and are tested.
- [x] Authentication, log ingestion/listing, idempotency, caching, and rate-limit code paths have targeted tests.
- [ ] Full `./gradlew test` is currently green on this machine. Current run failed because 7 Testcontainers-backed integration tests could not find/connect to a Docker environment.
- [ ] Roadmap and docs are fully synchronized with implementation. Core Week 1 testing docs exist; deeper perf, operations, and infrastructure docs are still missing.

Immediate priorities:

1. Fix local Docker/Testcontainers availability and rerun `./gradlew test`.
2. Finish Phase 1 documentation evidence: JPA/perf artifacts, README caching/performance section, and test-running docs.
3. Close remaining known security gaps from the threat model: tenant isolation and Redis hardening.
4. Complete Week 5 Redis/rate-limit deliverables with measurable k6 evidence.
5. Start Week 6 only after Phase 1 tests and docs are clean.

## Product Scope

Build a backend system that supports:

- S3 presigned uploads
- API publishing `LogUploaded` events to Kafka
- Worker-side asynchronous parsing and chunking
- Postgres persistence through JPA/Hibernate
- Keyword and semantic search with pgvector
- RAG Q&A with citations
- Redis caching and rate limiting
- Observability from logs to metrics, dashboards, tracing, cloud, and Kubernetes
- AWS ECS deployment with Terraform, followed by AWS EKS, HPA, and Helm

## Engineering Competency Goals

Build and maintain the capabilities that matter in production backend systems:

- HTTP API design and correctness
- Persistence modeling, transactions, and query performance
- Authentication, authorization, and threat modeling
- Event-driven architecture and asynchronous workers
- Caching, rate limiting, load testing, and resilience
- Observability, incident response, and operational runbooks
- Cloud deployment, infrastructure as code, and Kubernetes
- AI integration, evaluation, safety, and cost control
- Testing across unit, integration, contract, E2E, load, and operational layers

## Delivery Principles

- Keep `loglens-api` and `loglens-worker` independently buildable and deployable.
- Use Kafka for service-to-service workflow communication once asynchronous ingestion starts.
- Keep shared contracts under `libs/common/events/v1`; avoid sharing service internals between apps.
- Prefer explicit boundaries: DTOs for transport, entities for persistence, services for business logic, repositories for data access.
- Add tests at the lowest layer that proves the behavior; use full integration tests only when framework wiring or real infrastructure matters.
- Treat docs, ADRs, operational notes, and performance evidence as part of the deliverable, not optional polish.

## How To Use This Roadmap

The roadmap is an engineering plan, not just a feature backlog. Each week should produce working code, tests, documentation, and clear understanding of the tradeoffs.

For each week:

1. Read the topics and identify the main backend concept.
2. Implement the smallest production-shaped behavior that exercises that concept.
3. Write focused tests before broadening the implementation.
4. Run the narrowest useful test command while iterating.
5. Document what was learned, what tradeoffs were made, and what remains risky.
6. Commit small, self-contained changes.
7. Only mark checklist items complete when behavior, tests, and documentation are all present.

Avoid mixing weeks unless a later-week issue blocks the current week. If a later concern appears, add it as a note under the relevant future week and return to the current checkpoint.

## Review Checkpoints

At the end of each week, be able to explain:

- What backend problem this week addressed.
- Which files implement the behavior.
- Which tests prove it.
- Which failures or edge cases were discovered.
- Which tradeoffs were accepted.
- What a production team would still improve.

If these cannot be explained clearly, the week is not truly done even if the code compiles.

## Target Architecture

Two independent microservices:

### `loglens-api`

Synchronous service responsible for:

- Auth with JWT access tokens and refresh tokens
- Accepting log uploads
- Storing upload metadata
- Publishing events to Kafka
- Query endpoints
- AI endpoints

### `loglens-worker`

Asynchronous service responsible for:

- Consuming Kafka events
- Parsing log files
- Splitting logs into chunks
- Storing chunks
- Generating embeddings
- Handling retries and failures

Service rules:

- Separate Docker images
- Separate configs
- Separate health endpoints
- Kafka-based communication
- Shared contracts only in `libs/common/events/v1`

## Target Repository Structure

```text
loglens/
  apps/
    api/
    worker/
  libs/
    common/
  deploy/
    docker/
    k8s/
    helm/
  infra/
    terraform/
  docs/
    adr/
    architecture/
    api/
    perf/
    incidents/
    security/
    cost/
    evals/
    runbook/
    observability/
    testing/
    roadmap/
  openapi.yaml
  README.md
```

## Global Testing Strategy

Testing layers to build progressively:

- [ ] Unit tests for services, mappers, validators
- [ ] Slice tests for controllers and repositories
- [ ] Integration tests with real Postgres, Redis, and Kafka through Testcontainers
- [ ] Contract tests for event schema compatibility
- [ ] E2E tests for upload to processing to search to RAG
- [ ] Load tests with k6 locally, through ALB, and on EKS
- [ ] Resilience tests for retries, circuit breakers, idempotency, DLQ, and replay
- [ ] Observability tests for correlation IDs, metrics, and trace propagation

Testing documents to maintain:

- [x] `docs/testing/testing-strategy.md`
- [ ] `docs/testing/test-pyramid.md`
- [ ] `docs/testing/how-to-run-tests.md`
- [ ] CI workflow running unit and integration tests on PRs

Reference materials:

- Spring Boot testing: https://docs.spring.io/spring-boot/reference/testing/
- Testcontainers: https://java.testcontainers.org/
- GitHub Actions: https://docs.github.com/en/actions
- k6: https://k6.io/docs/

## Quality Gates

Before marking a week complete:

- [ ] Main user-facing behavior works locally
- [ ] Relevant automated tests pass
- [ ] Docker or infrastructure files are reproducible from a clean checkout
- [ ] README section is updated
- [ ] ADR exists for major architecture decisions
- [ ] Operational or security risk is documented when applicable
- [ ] Any screenshots, reports, or performance evidence are committed under `docs/`

## Phase 1: Backend Core And Local Microservices

### Week 1: Dockerized Scaffold, RFC7807, Structured Logging, Testing Baseline

Learn:

- Docker and Docker Compose
- Spring Boot fundamentals
- Spring bean wiring
- Logback and structured logging
- RFC7807 problem details
- Spring testing
- ADRs

References:

- Docker: https://docs.docker.com/get-started/
- Docker Compose: https://docs.docker.com/compose/
- Spring Boot web: https://docs.spring.io/spring-boot/reference/web/
- Spring bean wiring: https://docs.spring.io/spring-framework/reference/core/beans.html
- Spring logging: https://docs.spring.io/spring-boot/reference/features/logging.html
- Logback configuration: https://logback.qos.ch/manual/configuration.html
- RFC7807: https://datatracker.ietf.org/doc/html/rfc7807
- ADRs: https://adr.github.io/

Deliverables:

- [x] `apps/api` boots and exposes `GET /health`
- [x] `apps/worker` boots and exposes `GET /health`
- [x] `apps/api/Dockerfile`
- [x] `apps/worker/Dockerfile`
- [x] `deploy/docker/docker-compose.local.yml` runs local services
- [x] RFC7807 `ProblemDetail` error responses
- [x] `GlobalExceptionHandler.kt`
- [x] Validated request DTO on API endpoint
- [x] JSON logs through `logback-spring.xml` in API and worker
- [x] Correlation/request ID filter using MDC
- [x] Echo header `X-Request-Id`
- [x] `/health` integration test for API
- [x] Validation integration test for API
- [x] `docs/testing/testing-strategy.md` v1
- [x] `docs/adr/0001-layered-architecture.md`
- [x] README skeleton with What, Architecture v1, Local Dev, and ADRs

### Week 2: JPA/Hibernate, Transactions, DB Integration Testing

Learn:

- Spring Data JPA
- Hibernate entity lifecycle
- Transactions
- Fetching and N+1 query problems
- Optimistic locking
- JDBC batching
- Postgres `EXPLAIN`
- Testcontainers Postgres

References:

- Spring Data JPA: https://docs.spring.io/spring-data/jpa/reference/
- Hibernate user guide: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html
- Hibernate entity states: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#pc-states
- Spring transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction.html
- Hibernate fetching: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching
- N+1 query problem: https://vladmihalcea.com/n-plus-1-query-problem/
- Optimistic locking: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic
- Hibernate batching: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch
- Postgres EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
- Testcontainers Postgres: https://java.testcontainers.org/modules/databases/postgres/

Deliverables:

- [x] `UserEntity`
- [x] `RefreshTokenEntity`
- [x] `LogEntity`
- [x] `LogChunkEntity`
- [x] Lazy relationships with explicit fetch plan through fetch join or `@EntityGraph`
- [x] `@Transactional` service method
- [x] `@Version` optimistic locking on one entity
- [x] Hibernate batch config with `hibernate.jdbc.batch_size=20`
- [x] Intentional N+1 demonstration
- [x] N+1 fix
- [x] `docs/perf/n-plus-one-before.txt`
- [x] `docs/perf/n-plus-one-after.txt`
- [ ] `docs/perf/explain-before.txt`
- [ ] `docs/perf/explain-after.txt`
- [x] Testcontainers DB integration test
- [x] Transaction rollback test
- [x] Optimistic lock conflict test
- [x] `docs/adr/0002-jpa-strategy.md`
- [x] README section for JPA/Hibernate decisions

### Week 3: Security And Security Testing

Learn:

- Spring Security
- JWT resource server concepts
- Password storage
- OWASP Top 10
- Refresh token rotation

References:

- Spring Security: https://docs.spring.io/spring-security/reference/
- Spring Security JWT resource server concepts: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Password storage: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- OWASP Top 10: https://owasp.org/www-project-top-ten/
- Refresh tokens: https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/

Deliverables:

- [x] `POST /auth/register`
- [x] `POST /auth/login`
- [x] `POST /auth/refresh`
- [x] Refresh token rotation stored in DB
- [x] RBAC security rule for `/v1/admin/**`
- [ ] Tenant isolation on user-owned log APIs
- [x] Login success test
- [x] Login failure test
- [x] Expired token rejected test
- [x] Refresh rotation invalidates old token test
- [x] RBAC forbidden test
- [ ] Tenant isolation HTTP integration tests
- [x] `docs/security/threat-model.md`
- [x] `docs/adr/0003-auth-strategy.md`
- [x] README security model

Notes:

- Refresh tokens are single-use: `/v1/auth/refresh` deletes the consumed refresh token, stores a replacement refresh token for the same user, and returns the replacement token to the client.
- Expired refresh tokens are deleted when encountered and do not produce replacement tokens.
- Authentication error responses use generic problem details so raw exception messages are logged server-side but not returned to clients.

### Week 4: API Maturity, OpenAPI, Idempotency, API Tests

Learn:

- API design
- OpenAPI
- springdoc
- HTTP semantics

References:

- API design: https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design
- OpenAPI specification: https://swagger.io/specification/
- springdoc: https://springdoc.org/
- HTTP semantics: https://www.rfc-editor.org/rfc/rfc9110.html

Deliverables:

- [x] `openapi.yaml`
- [ ] `docs/api/postman_collection.json`
- [x] Idempotency middleware/filter
- [x] `IdempotencyEntity`
- [x] `docs/api/deprecation-plan.md`
- [x] Idempotency integration test
- [x] Controller happy-path tests for 2 endpoints
- [x] `docs/adr/0004-versioning-strategy.md`
- [x] README API design section and OpenAPI link

### Week 5: Redis Caching, Rate Limiting, Load Testing Baseline

Learn:

- Redis
- Cache-aside pattern
- Rate limiting
- k6

References:

- Redis docs: https://redis.io/docs/latest/
- Redis rate limiting patterns: https://redis.io/redis-best-practices/basic-rate-limiting/
- k6 getting started: https://k6.io/docs/get-started/

Deliverables:

- [x] Redis added to Compose
- [x] Cache-aside on a hot endpoint
- [ ] Redis-backed rate limiter middleware
- [x] Integration test proving cache hit or cache set/get behavior
- [x] Rate limit test where N requests produces chosen limit status
- [ ] `docs/perf/k6-local.js`
- [ ] `docs/perf/k6-local-report.md` with p95, throughput, and error rate
- [x] `docs/adr/0005-caching-strategy.md`
- [ ] README performance and caching section with numbers

### Week 6: Event-Driven Architecture, Kafka, Contract Testing, CI

Learn:

- Kafka producers, consumers, and delivery semantics
- Spring Kafka
- Retry topics and error handling
- Kafka metrics
- Idempotent consumer pattern
- Transactional outbox concept
- Testcontainers Kafka
- GitHub Actions

References:

- Kafka introduction: https://kafka.apache.org/documentation/#introduction
- Kafka consumers: https://kafka.apache.org/documentation/#intro_consumers
- Kafka delivery semantics: https://kafka.apache.org/documentation/#semantics
- Spring Kafka: https://docs.spring.io/spring-kafka/reference/
- Spring Kafka receiving messages: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages.html
- Spring Kafka retry topics: https://docs.spring.io/spring-kafka/reference/retrytopic.html
- Spring Kafka error handling: https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html
- Spring Kafka metrics: https://docs.spring.io/spring-kafka/reference/appendix/micrometer.html
- Idempotent consumer pattern: https://martinfowler.com/articles/patterns-of-distributed-systems/idempotent-consumer.html
- Transactional outbox pattern: https://microservices.io/patterns/data/transactional-outbox.html
- Testcontainers Kafka: https://java.testcontainers.org/modules/kafka/
- GitHub Actions: https://docs.github.com/en/actions

Deliverables:

- [ ] Kafka added to Compose with API, worker, Postgres, Redis, and Kafka
- [ ] `libs/common/events/v1/LogUploadedEvent.kt`
- [ ] API publishes `LogUploaded` from `/logs/{id}/ingest`
- [ ] Worker consumes and processes into DB
- [ ] Retry and DLQ topics configured
- [ ] `ProcessedEventEntity` with unique `eventId`
- [ ] `docs/architecture/event-flow.png`
- [ ] Micrometer counter `logs_uploaded_total`
- [ ] Micrometer counter `logs_processed_total`
- [ ] Micrometer counter `kafka_consumer_errors_total`
- [ ] Kafka integration test proving publish to consume
- [ ] Contract test for event JSON serialization compatibility
- [ ] Replay test where the same event twice is idempotent
- [ ] GitHub Actions workflow runs unit tests
- [ ] GitHub Actions workflow runs integration tests with Testcontainers
- [ ] `docs/adr/0006-event-driven-architecture.md`
- [ ] README event-driven architecture section

## Phase 2: Reliability, Observability, And Cloud

### Week 7: Resilience And Resilience Testing

Learn:

- Resilience4j circuit breakers
- Retries
- Time limiters
- Bulkheads
- AWS retry guidance

References:

- Resilience4j: https://resilience4j.readme.io/
- Circuit breakers: https://resilience4j.readme.io/docs/circuitbreaker
- Retry: https://resilience4j.readme.io/docs/retry
- TimeLimiter: https://resilience4j.readme.io/docs/timelimiter
- Bulkhead: https://resilience4j.readme.io/docs/bulkhead
- AWS retry guidance: https://docs.aws.amazon.com/general/latest/gr/api-retries.html

Deliverables:

- [ ] Circuit breaker applied to external calls
- [ ] Retry applied to external calls
- [ ] Timeout applied to external calls
- [ ] S3 stub is acceptable for first implementation
- [ ] `docs/failure-matrix.md`
- [ ] Circuit opens after failures test
- [ ] Retries happen test
- [ ] Timeout enforced test
- [ ] `docs/adr/0007-resilience-policy.md`
- [ ] README reliability section

### Week 8: Observability Stack, Incident Drill, Observability Tests

Learn:

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Metric types
- RED method
- Hibernate statistics

References:

- Spring Boot Actuator: https://docs.spring.io/spring-boot/reference/actuator/
- Micrometer: https://micrometer.io/docs
- Prometheus: https://prometheus.io/docs/introduction/overview/
- Prometheus metric types: https://prometheus.io/docs/concepts/metric_types/
- RED method: https://www.weave.works/blog/the-red-method-key-metrics-for-microservices-architecture/
- Hibernate statistics: https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#statistics

Deliverables:

- [ ] `/actuator/prometheus` enabled for API
- [ ] `/actuator/prometheus` enabled for worker
- [ ] Prometheus added to Compose
- [ ] Grafana added to Compose
- [ ] `docs/architecture/grafana-dashboard.png`
- [ ] Hibernate statistics enabled
- [ ] Grafana panel for query count or DB call rate
- [ ] Simulated incident: DB pool exhaustion, consumer crash, or lag spike
- [ ] `docs/incidents/001-incident.md`
- [ ] Metrics endpoint returns 200 smoke test
- [ ] Metrics endpoint contains custom counters smoke test
- [ ] Correlation ID header present smoke test
- [ ] `docs/adr/0008-observability-stack.md`
- [ ] README local observability section

### Week 9: AWS Manual Deploy, S3 Presigned Uploads, Cloud Sanity Tests

Learn:

- AWS practitioner fundamentals
- S3 presigned URLs
- EC2
- RDS Postgres
- IAM
- VPC basics

References:

- AWS Cloud Practitioner essentials: https://aws.amazon.com/training/digital/aws-cloud-practitioner-essentials/
- S3 presigned URLs: https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html
- EC2 getting started: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html
- RDS Postgres getting started: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_GettingStarted.CreatingConnecting.PostgreSQL.html
- IAM introduction: https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html
- VPC overview: https://docs.aws.amazon.com/vpc/latest/userguide/what-is-amazon-vpc.html

Deliverables:

- [ ] Real S3-backed `POST /uploads/presign`
- [ ] EC2 deploy through Docker
- [ ] RDS connected
- [ ] `docs/cloud/manual-smoke-test.md`
- [ ] Health endpoint reachable in cloud
- [ ] Upload presign works in cloud
- [ ] DB connection works in cloud
- [ ] `docs/cloud/manual-deploy.md`
- [ ] `docs/adr/0009-aws-manual-first.md`
- [ ] README cloud manual section

### Week 10: ECS, ALB, CloudWatch, Cloud Load Testing

Learn:

- ECS
- ECR
- ALB
- ECS logs with `awslogs`
- CloudWatch Logs
- CloudWatch Monitoring
- k6

References:

- ECS: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html
- ECR image push: https://docs.aws.amazon.com/AmazonECR/latest/userguide/docker-push-ecr-image.html
- ALB: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/introduction.html
- ECS `awslogs`: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_awslogs.html
- CloudWatch Logs: https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/WhatIsCloudWatchLogs.html
- CloudWatch monitoring: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/WhatIsCloudWatch.html
- k6: https://k6.io/docs/

Deliverables:

- [ ] API deployed as ECS service
- [ ] Worker deployed as ECS service
- [ ] ALB routes to API
- [ ] Separate CloudWatch log groups
- [ ] `docs/cloud/cloudwatch-observability.md`
- [ ] CloudWatch alarm plan for 5xx, latency, CPU, memory, worker failures, and lag
- [ ] `docs/perf/k6-alb.js`
- [ ] `docs/perf/k6-alb-report.md`
- [ ] `docs/adr/0010-ecs-architecture.md`
- [ ] README ECS deployment section

### Week 11: Terraform IaC And Infra Sanity Tests

Learn:

- Terraform AWS provider
- Terraform state
- S3 backend
- Terraform modules
- `terraform plan`

References:

- Terraform AWS tutorials: https://developer.hashicorp.com/terraform/tutorials/aws-get-started
- Terraform state: https://developer.hashicorp.com/terraform/language/state
- S3 backend: https://developer.hashicorp.com/terraform/language/settings/backends/s3
- Terraform modules: https://developer.hashicorp.com/terraform/language/modules
- Terraform plan: https://developer.hashicorp.com/terraform/cli/commands/plan

Deliverables:

- [ ] VPC through Terraform
- [ ] RDS through Terraform
- [ ] ECS through Terraform
- [ ] ALB through Terraform
- [ ] Remote state with S3 and locking
- [ ] `docs/cloud/terraform-apply.md`
- [ ] `docs/testing/infra-sanity.md`
- [ ] Terraform outputs present
- [ ] ALB reachable
- [ ] ECS service stable
- [ ] `docs/adr/0011-terraform-structure.md`
- [ ] README infrastructure as code section

## Phase 3: AI, RAG, And Advanced Observability

### Week 12: LLM Integration And AI Safety Tests

Learn:

- OpenAI API
- Structured outputs
- Production best practices
- OWASP LLM Top 10

References:

- OpenAI docs: https://platform.openai.com/docs
- Structured outputs: https://platform.openai.com/docs/guides/structured-outputs
- Production best practices: https://platform.openai.com/docs/guides/production-best-practices
- OWASP LLM Top 10: https://owasp.org/www-project-top-10-for-large-language-model-applications/

Deliverables:

- [ ] `POST /ai/analyze` returns validated structured JSON
- [ ] `TokenUsageEntity`
- [ ] Quotas
- [ ] Contract test for output schema
- [ ] Quota enforcement test
- [ ] `docs/security/llm-threats.md`
- [ ] `docs/testing/llm-safety-cases.md`
- [ ] Unsafe input test cases documented
- [ ] `docs/adr/0012-llm-strategy.md`
- [ ] README AI integration section

### Week 13: Embeddings, RAG, Distributed Tracing

Learn:

- pgvector
- pgvector indexing
- RAG
- OpenTelemetry Java
- Jaeger

References:

- pgvector: https://github.com/pgvector/pgvector
- pgvector indexing: https://github.com/pgvector/pgvector#indexing
- RAG overview: https://www.pinecone.io/learn/retrieval-augmented-generation/
- OpenTelemetry Java: https://opentelemetry.io/docs/languages/java/
- OpenTelemetry Java instrumentation: https://opentelemetry.io/docs/zero-code/java/
- Jaeger: https://www.jaegertracing.io/docs/latest/

Deliverables:

- [ ] pgvector migration
- [ ] `Embedding` model
- [ ] `/semantic-search`
- [ ] `/rag/ask` returns answer with chunk ID citations
- [ ] `docs/evals/golden-questions.md`
- [ ] Jaeger added to Compose
- [ ] OpenTelemetry enabled for API
- [ ] OpenTelemetry enabled for worker
- [ ] Trace context propagated from API to Kafka headers to worker
- [ ] `docs/architecture/trace-flow.png`
- [ ] `docs/testing/tracing-verification.md`
- [ ] Automated lightweight check for `X-Request-Id` on Kafka messages
- [ ] Automated lightweight check for trace headers on Kafka messages
- [ ] `docs/adr/0013-rag-design.md`
- [ ] README RAG and tracing section

### Week 14: Capstone E2E, System Testing, Documentation Completeness

Learn:

- README quality and repository presentation
- System-level verification

References:

- GitHub README guidance: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes

Deliverables:

- [ ] End-to-end pipeline works: presign to upload to ingest to Kafka to worker to chunks to embeddings to semantic to RAG
- [ ] `docs/architecture/c4-context.png`
- [ ] `docs/architecture/c4-container.png`
- [ ] `docs/architecture/c4-component.png`
- [ ] `docs/runbook/production-runbook.md`
- [ ] `docs/cost/aws-cost-estimate.md`
- [ ] `docs/testing/e2e-test-plan.md`
- [ ] Automated happy-path E2E test through `docs/testing/e2e-happy-path.sh` or Kotlin test runner
- [ ] `docs/testing/release-checklist.md`
- [ ] `docs/adr/0014-final-architecture.md`
- [ ] README finalized with architecture, runbook, and cost links

## Phase 4: Kubernetes, Hardening, And Release Engineering

### Week 15: Local Kubernetes And Deployment Tests

Learn:

- Kubernetes overview
- kind

References:

- Kubernetes overview: https://kubernetes.io/docs/concepts/overview/
- kind: https://kind.sigs.k8s.io/

Deliverables:

- [ ] API manifest
- [ ] Worker manifest
- [ ] Service manifest
- [ ] ConfigMap
- [ ] Secret
- [ ] Local cluster running
- [ ] `docs/testing/k8s-local-smoke.md`
- [ ] `docs/testing/k8s-local-verify.sh`
- [ ] `docs/adr/0015-k8s-local.md`
- [ ] README local Kubernetes section

### Week 16: EKS, Ingress, Metrics Server, Operational Validation

Learn:

- EKS
- AWS Load Balancer Controller
- metrics-server

References:

- EKS: https://docs.aws.amazon.com/eks/latest/userguide/
- AWS Load Balancer Controller: https://kubernetes-sigs.github.io/aws-load-balancer-controller/
- metrics-server: https://github.com/kubernetes-sigs/metrics-server

Deliverables:

- [ ] EKS cluster created
- [ ] Ingress through AWS Load Balancer Controller
- [ ] metrics-server installed
- [ ] `docs/testing/eks-validation.md`
- [ ] Runbook ops note for EKS
- [ ] `docs/adr/0016-eks-architecture.md`
- [ ] README EKS deployment section

### Week 17: HPA, Resource Limits, DB Pool Tuning, Scaling Proof

Learn:

- HPA
- Kubernetes requests and limits
- Liveness, readiness, and startup probes
- HikariCP
- k6

References:

- HPA: https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/
- Requests and limits: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
- Probes: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
- HikariCP: https://github.com/brettwooldridge/HikariCP
- k6: https://k6.io/docs/

Deliverables:

- [ ] Requests and limits
- [ ] Liveness probes
- [ ] Readiness probes
- [ ] `deploy/k8s/hpa.yaml`
- [ ] `docs/perf/db-pool-tuning.md`
- [ ] `docs/perf/k6-eks.js`
- [ ] `docs/perf/k6-eks-scaling-report.md`
- [ ] Screenshots showing replicas scaling
- [ ] Regression note: what broke while scaling and how it was fixed
- [ ] `docs/adr/0017-k8s-hardening.md`
- [ ] README autoscaling proof section

### Week 18: Helm Packaging, Rollback, DR, Release Readiness

Learn:

- Helm
- Helm charts
- Values files
- Kubernetes deployment strategy

References:

- Helm: https://helm.sh/docs/
- Helm charts: https://helm.sh/docs/topics/charts/
- Helm values files: https://helm.sh/docs/chart_template_guide/values_files/
- Kubernetes deployment strategies: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#strategy

Deliverables:

- [ ] Helm chart for API and worker at `deploy/helm/loglens/`
- [ ] `docs/runbook/rollback-strategy.md`
- [ ] `docs/runbook/disaster-recovery.md`
- [ ] RDS snapshot recovery documented
- [ ] S3 versioning recovery documented
- [ ] `docs/architecture/ecs-vs-eks.md`
- [ ] `docs/testing/helm-smoke.md`
- [ ] `docs/testing/release-checklist.md` completed
- [ ] `docs/adr/0018-helm-deployment.md`
- [ ] README Helm, rollback, and DR section

## Suggested Product Improvements

These features would make LogLens feel more like a real product while reinforcing useful backend engineering concerns.

### Ingestion And Data Model

- [ ] Add `Project` or `Application` entity so users can separate logs by app/environment
- [ ] Add `Environment` field: `dev`, `staging`, `prod`
- [ ] Add severity normalization for `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`
- [ ] Support structured JSON logs and raw text logs
- [ ] Add parser plugins for common formats: Logback, Nginx, JSON Lines
- [ ] Add ingestion status lifecycle: `UPLOADED`, `QUEUED`, `PROCESSING`, `PROCESSED`, `FAILED`
- [ ] Track file checksum to deduplicate uploads
- [ ] Add retention policy per project

### Search And Analysis

- [ ] Add keyword search filters by service, environment, severity, timestamp range, and trace ID
- [ ] Add saved searches
- [ ] Add error grouping by message fingerprint
- [ ] Add basic anomaly detection for sudden error-rate spikes
- [ ] Add related-log discovery using trace ID, request ID, or semantic similarity
- [ ] Add RAG answer feedback: helpful, not helpful, wrong citation
- [ ] Add eval scoring for RAG answers against golden questions

### API And Developer Experience

- [ ] Add API keys for machine-to-machine ingestion
- [ ] Add OpenAPI validation in CI
- [ ] Add generated API examples with curl commands
- [ ] Add pagination standards for all list endpoints
- [ ] Add consistent sorting and filtering conventions
- [ ] Add a small CLI for upload, search, and ask commands
- [ ] Add seed data and demo scripts

### Security And Compliance

- [ ] Add per-user or per-project authorization boundaries
- [ ] Add audit log table for auth, uploads, deletes, and admin actions
- [ ] Add secret scanning in CI
- [ ] Add dependency vulnerability scanning
- [ ] Add PII redaction during parsing
- [ ] Add encryption-at-rest notes for S3, RDS, and backups
- [ ] Add data deletion workflow for a project or user

### Reliability And Operations

- [ ] Add transactional outbox before publishing Kafka events from DB-backed workflows
- [ ] Add poison-message handling policy for permanently bad log files
- [ ] Add replay tooling for DLQ events
- [ ] Add worker concurrency tuning docs
- [ ] Add SLOs: availability, ingestion latency, search latency, and RAG latency
- [ ] Add alerts tied to SLOs and error budgets
- [ ] Add backup restore drill, not only backup configuration
- [ ] Add migration rollback policy

### Cost And Performance

- [ ] Add ingestion size limits and compression support
- [ ] Add batch inserts for chunks and embeddings
- [ ] Add query timeout and max result limits
- [ ] Add pgvector index comparison notes
- [ ] Add cost guardrails for LLM calls
- [ ] Add monthly AWS cost budget and alarm
- [ ] Add performance budget per endpoint

## Engineering Practice Enhancements

Use these as stretch goals when the core roadmap is stable.

- [ ] Write one short engineering note after each week: what failed, what changed, and what tradeoff was accepted
- [ ] Record every production-style decision as an ADR, including rejected alternatives
- [ ] Keep a `docs/engineering-notes/` folder with explanations of tricky topics
- [ ] For every major feature, write down the failure modes before implementation
- [ ] For every new dependency, document why it is worth adding
- [ ] Add code review checklists for security, persistence, messaging, and observability
- [ ] Practice diagnosing issues from logs, metrics, traces, and database evidence before changing code

## Final Engineering Outcome

By the end, the repository should demonstrate:

- Microservices with Dockerized local development
- JPA/Hibernate skill through N+1 proof, fetch strategy, locking, and batching
- Event-driven architecture with Kafka, retries, DLQ, idempotency, and contracts
- Observability with logs, metrics, dashboards, tracing, cloud, and Kubernetes
- Cloud deployments through EC2, ECS, Terraform, and EKS
- AI engineering with structured outputs, quotas, RAG, and evals
- Testing across unit, integration, contract, E2E, load, and scaling proof
- Incident reports, runbooks, cost model, and ADRs
