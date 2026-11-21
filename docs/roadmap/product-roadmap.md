# LogLens Roadmap

LogLens is a log ingestion, search, and AI-analysis platform for engineering teams—a focused, production-ready alternative to the core workflows of Sentry or Datadog. The roadmap prioritizes a secure ingestion path, fast investigation, evidence-based AI assistance, and the operational controls required to run the system for real customers.

## Status

Roadmap structure revised: 2026-11-21. Implementation status below was last verified on 2026-11-13. **Phase 1 is nearly done.** `./gradlew test` is green (Docker/Testcontainers working). The API has JWT auth with refresh rotation, tenant-scoped log CRUD, idempotency, Redis cache-aside, per-IP rate limiting, JPA entities with fetch plans/locking/batching, OpenAPI, ADRs 0001-0005 and a threat model. The worker is a health-endpoint scaffold only.

Known gaps (fix these first, see Phase 1):

- `docs/perf/n-plus-one-before.txt` and `n-plus-one-after.txt` were checked off but do not exist.
- No CI (`.github/` is missing), no k6 baseline, rate limiter is in-memory (Bucket4j), not Redis-backed.
- Threat-model open items 12-14 (Redis auth, deserialization typing, maxmemory) not closed.

## Completed So Far (verified against the repo on 2026-11-13)

Verified means the file/test exists in the repo; test bodies were not all re-read.

**Foundation:** done
- [x] Multi-module Gradle build (api, worker, common) with convention plugin and version catalog
- [x] API and worker boot with `/health` + `HealthIT` for both; Dockerfiles for both; Compose with Postgres and Redis
- [x] RFC7807 `ProblemDetail` errors via `GlobalExceptionHandler` (sanitized, no internals leaked); validated request DTOs
- [x] JSON logging (`logback-spring.xml` in both apps); `CorrelationIdFilter` echoing `X-Request-Id` via MDC
- [x] ArchUnit tests enforcing layering; ADR 0001; `docs/testing/testing-strategy.md`; README skeleton

**Persistence:** mostly done
- [x] `UserEntity`, `RefreshTokenEntity`, `LogEntity`, `LogChunkEntity`; lazy relations; `JOIN FETCH` fix
- [x] `@Transactional` services; `@Version` optimistic locking on `LogEntity`
- [x] Intentional N+1 demo and fix, proven by SQL-count assertions in `LogServiceIT`; Testcontainers Postgres ITs
- [x] `docs/perf/explain-before.txt` / `explain-after.txt`; ADR 0002

**Security:** done
- [x] `register` / `login` / `refresh` with single-use refresh-token rotation stored in DB; BCrypt; required `JWT_SECRET`
- [x] RBAC rule for `/v1/admin/**`; `SecurityIT`, `AuthServiceTest`, `JwtServiceTest`, `JwtAuthenticationFilterTest`
- [x] Tenant isolation on log APIs incl. cache keys (key is `userId:id`; a `#id`-only key leaked entries across users), with HTTP and cache ITs
- [x] `docs/security/threat-model.md`; ADR 0003; README security model

**API maturity:** done except Postman
- [x] `openapi.yaml`; `IdempotencyFilter` + `IdempotencyKeyEntity` + ITs; `docs/api/deprecation-plan.md`; ADR 0004; README API section

**Caching and limits:** partially done
- [x] Redis in Compose; cache-aside on `getLogById` with cache ITs; per-IP `RateLimitFilter` (in-memory Bucket4j) + test; ADR 0005

**Evidence correction:** earlier planning marked several items complete without repository evidence: `docs/perf/n-plus-one-*.txt`, Hibernate `batch_size=20` configuration, transaction-rollback and optimistic-lock-conflict tests, and a substantive README JPA section. They remain open in Phase 1.

**Fixed 2026-11-13 (real-app startup):** the app did not start via Compose: Dockerfiles needed a host-built JAR, the Alpine Temurin images have no ARM build (the cause of the `linux/amd64` pin), `DATABASE_URL` was never read by Spring, and no schema existed outside tests. Now: multi-stage Dockerfiles, Flyway `V1__init.sql`, `ddl-auto: validate` everywhere, standard `SPRING_DATASOURCE_*` env vars. Verified: health 200, register 201, login 200 against Compose Postgres.

## What "Production-Grade" Means Here (the filter for every decision)

A feature is finished when LogLens could be sold with it:

1. **Correct and safe for many customers:** strict tenant isolation, authz on every path, no secrets in code, untrusted input (including logs fed to an LLM) handled deliberately.
2. **Reliable under load and failure:** ingestion never silently drops logs; retries are idempotent; failures land in a DLQ with a replay path; behavior is measured (p95, throughput, error rate), not assumed.
3. **Operable:** metrics, structured logs, alerts tied to SLOs, runbooks, and incident write-ups, so someone can run it at 3am.
4. **Reproducible:** CI on every PR, one-command local stack, migrations, infrastructure as code.
5. **Cost-aware and monetizable:** per-customer usage is metered, quotas exist, LLM/storage/compute cost per customer is known, and pricing can be derived from it.
6. **Demoable:** a live URL, seed data, and a short walkthrough of the core value (ingest logs, find errors, ask questions with citations).
7. **Documented and reviewable:** ADRs, metrics, incidents, and tradeoffs are recorded clearly enough for operators and contributors to evaluate them.

Scope is chosen by value to the product, not by tool fashion. Kubernetes is deferred until the product needs it (see Phase 7), not dropped.

## Delivery Map

The roadmap has three useful stopping points. Reaching one is success; do not delay sharing the project until every later phase is complete.

| Release | What it proves | Phases | Share it when |
|---|---|---|---|
| **v0.1 — Operational beta** | Secure ingestion, search, error grouping, and asynchronous processing work together. | 1–3 | A team can run the system locally, ingest logs, investigate an error group, and recover a failed event. |
| **v0.2 — AI-assisted investigation** | AI analysis is grounded in retrieved logs and governed by measurable quality, safety, and cost controls. | 4–5 | A public demo answers questions about logs with citations, evaluation results, and cost/latency numbers. |
| **v1 — Commercial trial** | The system supports early organizations with managed access, quotas, data lifecycle controls, and test-mode billing. | 6 | An organization can sign up, ingest data safely, understand its quota, and complete a test-mode billing flow. |

**Work in thin vertical slices.** A slice includes the API contract, migration, implementation, focused test, and a short explanation. Do not build a large generic platform before a user-visible path needs it. Record blockers and move to the next independent item instead of widening scope.

## Rules

- Finish a phase before starting the next. A checkbox is ticked only when code, tests, and docs all exist and **the file actually exists in git**.
- Add tests at the lowest layer that proves the behavior; integration tests only when real wiring/infrastructure matters.
- Keep `apps/api` and `apps/worker` independently deployable; share only contracts under `libs/common/events/v1`.
- Review checkpoint at the end of each milestone: record the problem solved, affected components, proof of behavior, known limitation, and follow-up decision.
- After each milestone, add or update the relevant ADR, runbook, test guide, or release note so operational knowledge stays with the code.

## Phase 1: Release-Ready Core (target: 2 weeks)

Goal: a new contributor can start, test, and understand the current API without relying on undocumented local knowledge.

### Required for v0.1 baseline

- [x] Make the real app run via Compose (datasource config, Flyway migrations, multi-stage Dockerfiles)
- [ ] Automated smoke test that boots the Compose stack (run it in CI)
- [ ] GitHub Actions: build + unit + integration tests on every PR (Docker is available on runners)
- [x] Close threat-model items 12-14 (Redis password, concrete DTO cache serializer, maxmemory + LRU)
- [ ] README: architecture v1, exact run/test instructions, security model, and one honest limitations section
- [ ] Postman/Bruno collection or curl examples under `docs/api/`

### Evidence and quality backlog

- [ ] Produce the missing evidence: `docs/perf/n-plus-one-before.txt` / `n-plus-one-after.txt` (Hibernate SQL/statistics output, with query counts)
- [ ] Add `hibernate.jdbc.batch_size=20` and prove batching; add the missing transaction-rollback and optimistic-lock-conflict tests; README JPA section
- [ ] Redis-backed rate limiter (replace in-memory Bucket4j, or Bucket4j with Redis backend); test N requests -> 429 across two app instances
- [ ] k6 script `docs/perf/k6-local.js` + `docs/perf/k6-local-report.md` (p95, RPS, error rate, cache hit vs miss)
- [ ] `docs/testing/how-to-run-tests.md`

**Exit check:** on a clean machine or CI runner, the Compose smoke test passes; a reviewer can follow the README to register, create a log, and retrieve it; open security limitations are either fixed or visibly documented.

**Technical scope:** repeatable delivery, cache behavior, distributed rate limiting, query analysis, and load-test metrics.

## Phase 2: Make It a Real Log Product (target: 3 weeks)

Goal: the domain is believable. A user can send a meaningful batch of logs, narrow them down, and identify the errors that matter.

Build in this order so each step produces a usable feature:

### 2a: Model and ingest

- [ ] `Project` (application) + `environment` (dev/staging/prod); logs belong to a project; authorization at project level
- [ ] Severity normalization (TRACE..FATAL); structured JSON and raw-text ingestion
- [ ] Batch ingest endpoint (`POST /v1/projects/{id}/logs:batch`) with size limits and JDBC batching evidence

### 2b: Find the important logs

- [ ] Search: filters by service, level, time range, trace ID; **Postgres full-text search** (`tsvector` + GIN index) with `EXPLAIN` before/after
- [ ] Cursor/keyset pagination on all list endpoints (explain why offset pagination degrades)
- [ ] Error grouping by message fingerprint (the "Sentry" feature; the core demo moment: thousands of lines collapse into a few distinct errors)
- [ ] ADR: data model + search strategy; update OpenAPI
- [ ] Tests: tenant/project isolation, pagination edge cases, search correctness, query-plan assertion for indexed search

**Exit check:** a seeded example can ingest hundreds of logs, return a paginated filtered search, and group repeated failures. The README shows the exact requests and the measured query-plan evidence.

**Technical scope:** relational modeling, index selection, keyset pagination, migrations, and authorization boundaries.

## Phase 3: Reliable Async Pipeline (target: 4 weeks, three milestones)

Goal: the worker does real work. Ingestion must absorb bursts without blocking clients or losing logs, which is the product's core reliability promise.

**3a: Plumbing (week 1)**
- [ ] Kafka in Compose; `LogUploadedEvent` in `libs/common/events/v1`
- [ ] API publishes after ingest; worker consumes and writes derived data (parsed fields / fingerprints)
- [ ] Kafka Testcontainers integration test: publish -> consume -> DB row
- [ ] Contract test for event JSON compatibility

**3b: Correctness (week 2)**
- [ ] Idempotent consumer: `ProcessedEvent` with unique `eventId`; replaying the same event twice is a no-op (test)
- [ ] Retry topics + DLQ; poison-message policy; replay procedure documented
- [ ] Transactional outbox in the API (explain the dual-write problem it solves, with a failure-scenario test)

**3c: Operations (weeks 3–4)**
- [ ] Micrometer metrics: `logs_uploaded_total`, `logs_processed_total`, `kafka_consumer_errors_total`, consumer lag
- [ ] Actuator + Prometheus + Grafana in Compose; one dashboard screenshot committed
- [ ] Resilience4j (timeout/retry/circuit breaker) on one external call + `docs/failure-matrix.md`
- [ ] One simulated incident (kill the consumer, build lag) -> `docs/incidents/001-*.md` written as a real postmortem
- [ ] ADRs: event-driven architecture, resilience policy, observability stack

**Exit check:** the API can accept an event, the worker produces one visible derived result, a duplicate does not create another result, and a deliberately bad event reaches a documented DLQ/replay path. The dashboard answers whether the pipeline is healthy.

**Technical scope:** delivery semantics, idempotency, transactional outbox, consumer offsets, backpressure, and RED metrics.

## Phase 4: AI Engineering (target: 4 weeks, three milestones)

Goal: RAG over the logs, measured and controlled. Keep the LLM provider behind one small interface (Claude or OpenAI, swappable) so cost/safety logic lives in your code.

**4a: LLM analysis**
- [ ] `POST /v1/ai/analyze`: summarize an error group / time window, returns **schema-validated structured output**
- [ ] Token usage tracking per user, quotas, hard cost guardrail; quota-enforcement test
- [ ] Prompt-injection handling: logs are untrusted input. Document cases in `docs/testing/llm-safety-cases.md` and `docs/security/llm-threats.md` (OWASP LLM Top 10)

**4b: Embeddings + semantic search**
- [ ] pgvector migration; worker generates embeddings for chunks (batched, idempotent, retried)
- [ ] `/v1/search/semantic`; HNSW vs IVFFlat comparison notes with measured recall/latency

**4c: RAG + evals**
- [ ] `/v1/rag/ask` returns answer with chunk-ID citations; refuses when retrieval is weak
- [ ] `docs/evals/golden-questions.md` + an automated eval script (retrieval hit rate, citation correctness, groundedness) with a committed results table
- [ ] Compare at least two chunking or prompting variants using the eval numbers so decisions are made on evidence
- [ ] ADRs: LLM strategy, RAG design; README AI section with cost-per-query and latency numbers

**Exit check:** for a committed, non-sensitive test dataset, the system returns structured analysis and cited answers. The evaluation report shows where it succeeds, where it refuses, and what each query costs.

**Technical scope:** structured outputs, embeddings, vector indexes, retrieval evaluation, prompt-injection resistance, and cost control.

## Phase 5: Deploy and Operate (target: 2 weeks)

Goal: a public, monitored deployment with a demo. One cloud path, done properly (ECS first; Phase 7 covers scale-out).

- [ ] Deploy API + worker + managed Postgres/Redis (+ Kafka alternative if cost-prohibitive, documented) on AWS ECS Fargate **or** a simpler platform if cost/time demands; choose in an ADR
- [ ] Terraform for the deployed pieces (VPC/RDS/ECS/ALB) with remote state; `terraform plan` output documented
- [ ] S3 presigned uploads (real) and cloud smoke test script
- [ ] Cloud k6 run + report; CloudWatch logs/alarms basics
- [ ] End-to-end happy-path test: upload -> ingest -> Kafka -> worker -> embeddings -> search -> RAG answer
- [ ] C4 context + container diagrams; `docs/runbook/production-runbook.md`; `docs/cost/aws-cost-estimate.md` with a budget alarm
- [ ] Demo seed data + 3-minute demo video/GIF in README
- [ ] README finalized: what/why, architecture, live link, numbers, tradeoffs, known limits

**Exit check:** a stranger can open the live demo, follow the happy path using seed data, and find the production runbook. A failed health check or budget threshold has a defined response.

## Phase 6: Productization and Monetization (target: 4–6 weeks)

Goal: turn the system into something customers can sign up for, use, and pay for.

- [ ] Tenancy model: organizations/teams with members and roles on top of projects (project-level RBAC); invite flow
- [ ] API keys for machine ingestion (hashed at rest, scoped to a project, revocable, rate-limited per key)
- [ ] Plans and quotas: ingestion volume, retention days, AI queries per month; enforced in the API with clear `429`/`402`-style problem responses
- [ ] Usage metering: per-organization counters for ingested bytes, stored GB, and LLM tokens (idempotent, auditable)
- [ ] Billing via Stripe: subscription plans, checkout, webhooks (verified, idempotent), plan changes, dunning states; test mode only until Phase 5 is stable
- [ ] Retention policies per plan (scheduled deletion job) and data-deletion workflow for an organization/user (privacy and compliance)
- [ ] Audit log for auth, key, billing and admin actions
- [ ] PII redaction during parsing (configurable)
- [ ] SLOs (availability, ingestion latency, search latency, RAG latency) with alerts and an error-budget note
- [ ] Pricing note `docs/cost/pricing-model.md`: unit costs (per GB ingested/stored, per AI query) from real measurements, a margin target, and a price proposal
- [ ] ADRs: tenancy model, metering and quota design, billing integration

**Exit check:** use test-mode data to create an organization, invite a member, ingest with a scoped API key, observe a quota, and complete an idempotent billing-webhook flow. Do not accept real payments until privacy, retention, and support expectations are deliberately defined.

**Technical scope:** multi-tenant authorization, metering correctness (exactly-once-ish counting), webhook handling, billing state machines, data lifecycle, and unit economics.

## Phase 7: Scale-Out and Hardening (conditional)

Start only when measured load or reliability needs exceed what Phase 5's deployment handles.

- [ ] Kubernetes locally (kind): manifests, ConfigMap/Secret, probes, requests/limits
- [ ] EKS with ingress and metrics-server; HPA with a k6 scaling proof and a note on what broke while scaling (DB pool, consumer concurrency)
- [ ] Helm chart, rollback strategy, disaster-recovery runbook (RDS snapshot restore drill, S3 versioning recovery)
- [ ] `docs/architecture/ecs-vs-eks.md`: an honest comparison from running both
- [ ] Distributed tracing with OpenTelemetry (API -> Kafka headers -> worker) if cross-service debugging has become painful
- [ ] Dependency vulnerability and secret scanning in CI (cheap; pull into Phase 1 if convenient)

## Customer-Driven Enhancements

CLI for upload/search/ask, saved searches, anomaly detection on error-rate spikes, additional parser plugins (Nginx, Logback), related-log discovery, RAG answer feedback, multi-region.

## Execution Cadence

Use this to keep delivery focused and evidence-based:

1. Pick **one** checkbox that advances the current phase's exit check.
2. Write a one-sentence hypothesis: “When I finish this, a user can …”
3. Implement the smallest end-to-end slice, then run the focused test before the full suite.
4. Capture one piece of evidence: a test name, API response, query plan, metric, screenshot, or benchmark result.
5. Update the relevant operational or design documentation and make an atomic commit that explains the problem and result.
6. Once a week, update only the status and the next three actions. Move unplanned work to “Customer-Driven Enhancements.”

If a phase runs longer than its estimate, reduce optional scope before extending the deadline. The exit checks are the commitment; the target duration is a planning aid, not a reason to rush fragile work.
