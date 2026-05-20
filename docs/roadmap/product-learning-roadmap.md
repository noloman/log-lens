# LogLens Product And Backend Engineering Roadmap

LogLens is a learning project with a product goal: build a small but credible backend system for uploading, processing, storing, and analyzing application logs. The project is intentionally scoped like a compact Datadog, Sentry, or Logtail backend so it teaches production backend engineering through real product work.

Use this document as the working checklist. Keep items checked only when the code, tests, and documentation are all in place.

## Product Goal

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

## Learning Goal

Become a strong backend engineer by practicing the skills that matter in production:

- HTTP API design and correctness
- Persistence modeling, transactions, and query performance
- Authentication, authorization, and threat modeling
- Event-driven architecture and asynchronous workers
- Caching, rate limiting, load testing, and resilience
- Observability, incident response, and operational runbooks
- Cloud deployment, infrastructure as code, and Kubernetes
- AI integration, evaluation, safety, and cost control
- Testing across unit, integration, contract, E2E, load, and operational layers

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

- [ ] `docs/testing/testing-strategy.md`
- [ ] `docs/testing/test-pyramid.md`
- [ ] `docs/testing/how-to-run-tests.md`
- [ ] CI workflow running unit and integration tests on PRs

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

Deliverables:

- [ ] `apps/api` boots and exposes `GET /health`
- [ ] `apps/worker` boots and exposes `GET /health`
- [ ] `apps/api/Dockerfile`
- [ ] `apps/worker/Dockerfile`
- [ ] `deploy/docker/docker-compose.local.yml` runs Postgres only
- [ ] RFC7807 `ErrorResponse.kt`
- [ ] `GlobalExceptionHandler.kt`
- [ ] Temporary validation example endpoint
- [ ] JSON logs through `logback-spring.xml` in API and worker
- [ ] Correlation/request ID filter using MDC
- [ ] Echo header `X-Request-Id`
- [ ] `/health` integration test for API
- [ ] Validation integration test for API
- [ ] `docs/testing/testing-strategy.md` v1
- [x] `docs/adr/0001-layered-architecture.md`
- [ ] README skeleton with What, Architecture v1, Local Dev, and ADRs

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

Deliverables:

- [ ] `UserEntity`
- [ ] `RefreshTokenEntity`
- [ ] `LogEntity`
- [ ] `LogChunkEntity`
- [ ] Lazy relationships with explicit fetch plan through fetch join or `@EntityGraph`
- [ ] `@Transactional` service method
- [ ] `@Version` optimistic locking on one entity
- [ ] Hibernate batch config with `hibernate.jdbc.batch_size=20`
- [ ] Intentional N+1 demonstration
- [ ] N+1 fix
- [ ] `docs/perf/n-plus-one-before.txt`
- [ ] `docs/perf/n-plus-one-after.txt`
- [ ] `docs/perf/explain-before.txt`
- [ ] `docs/perf/explain-after.txt`
- [ ] Testcontainers DB integration test
- [ ] Transaction rollback test
- [ ] Optimistic lock conflict test
- [ ] `docs/adr/0002-jpa-strategy.md`
- [ ] README section for JPA/Hibernate decisions

### Week 3: Security And Security Testing

Learn:

- Spring Security
- JWT resource server concepts
- Password storage
- OWASP Top 10
- Refresh token rotation

Deliverables:

- [ ] `POST /auth/signup`
- [ ] `POST /auth/login`
- [ ] `POST /auth/refresh`
- [ ] Refresh token rotation stored in DB
- [ ] RBAC example endpoint
- [ ] Login success integration test
- [ ] Login failure integration test
- [ ] Expired token rejected test
- [ ] Refresh rotation invalidates old token test
- [ ] RBAC forbidden test
- [x] `docs/security/threat-model.md`
- [x] `docs/adr/0003-auth-strategy.md`
- [x] README security model

### Week 4: API Maturity, OpenAPI, Idempotency, API Tests

Learn:

- API design
- OpenAPI
- springdoc
- HTTP semantics

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

Deliverables:

- [ ] Redis added to Compose
- [ ] Cache-aside on a hot endpoint
- [ ] Redis rate limiter middleware
- [ ] Integration test proving cache hit or cache set/get behavior
- [ ] Rate limit test where N requests produces chosen limit status
- [ ] `docs/perf/k6-local.js`
- [ ] `docs/perf/k6-local-report.md` with p95, throughput, and error rate
- [ ] `docs/adr/0005-caching-strategy.md`
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

These features would make LogLens feel more like a real product while still teaching valuable backend engineering.

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

## Suggested Learning Enhancements

Use these as stretch goals when the core roadmap feels too easy.

- [ ] Write one short engineering note after each week: what was confusing, what failed, what changed
- [ ] Record every production-style decision as an ADR, including rejected alternatives
- [ ] Keep a `docs/interview-notes/` folder with explanations of tricky topics
- [ ] For every major feature, write down the failure modes before implementation
- [ ] For every new dependency, document why it is worth adding
- [ ] Add code review checklists for security, persistence, messaging, and observability
- [ ] Practice diagnosing issues from logs, metrics, traces, and database evidence before changing code

## Outcome

By the end, the repository should demonstrate:

- Microservices with Dockerized local development
- JPA/Hibernate skill through N+1 proof, fetch strategy, locking, and batching
- Event-driven architecture with Kafka, retries, DLQ, idempotency, and contracts
- Observability with logs, metrics, dashboards, tracing, cloud, and Kubernetes
- Cloud deployments through EC2, ECS, Terraform, and EKS
- AI engineering with structured outputs, quotas, RAG, and evals
- Testing across unit, integration, contract, E2E, load, and scaling proof
- Incident reports, runbooks, cost model, and ADRs
