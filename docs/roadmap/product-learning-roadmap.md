# LogLens Roadmap

LogLens is a small log ingestion, search, and AI-analysis backend (think compact Sentry/Datadog). It is a **learning project with a career goal**: become a hireable backend / AI engineer. Every phase is chosen because it produces something a hiring manager can see and an interviewer can probe.

Engineering method: [`docs/engineering/backend-engineering-guide.md`](../engineering/backend-engineering-guide.md).

## Status

Last reviewed: 2026-11-13. **Phase 1 is nearly done.** `./gradlew test` is green (Docker/Testcontainers working). The API has JWT auth with refresh rotation, tenant-scoped log CRUD, idempotency, Redis cache-aside, per-IP rate limiting, JPA entities with fetch plans/locking/batching, OpenAPI, ADRs 0001-0005 and a threat model. The worker is a health-endpoint scaffold only.

Known gaps (fix these first, see Phase 1):

- Tenant-isolation work (including a cache-key leak fix: key was `#id`, now `userId:id`) is uncommitted.
- `docs/perf/n-plus-one-before.txt` and `n-plus-one-after.txt` were checked off but do not exist.
- No CI (`.github/` is missing), no k6 baseline, rate limiter is in-memory (Bucket4j), not Redis-backed.
- Threat-model open items 12-14 (Redis auth, deserialization typing, maxmemory) not closed.

## Completed So Far (verified against the repo on 2026-11-13)

Verified means the file/test exists in the repo; test bodies were not all re-read.

**Foundation (old Week 1):** done
- [x] Multi-module Gradle build (api, worker, common) with convention plugin and version catalog
- [x] API and worker boot with `/health` + `HealthIT` for both; Dockerfiles for both; Compose with Postgres and Redis
- [x] RFC7807 `ProblemDetail` errors via `GlobalExceptionHandler` (sanitized, no internals leaked); validated request DTOs
- [x] JSON logging (`logback-spring.xml` in both apps); `CorrelationIdFilter` echoing `X-Request-Id` via MDC
- [x] ArchUnit tests enforcing layering; ADR 0001; `docs/testing/testing-strategy.md`; README skeleton

**Persistence (old Week 2):** mostly done
- [x] `UserEntity`, `RefreshTokenEntity`, `LogEntity`, `LogChunkEntity`; lazy relations; `JOIN FETCH` fix
- [x] `@Transactional` services; `@Version` optimistic locking on `LogEntity`
- [x] Intentional N+1 demo and fix, proven by SQL-count assertions in `LogServiceIT`; Testcontainers Postgres ITs
- [x] `docs/perf/explain-before.txt` / `explain-after.txt`; ADR 0002

**Security (old Week 3):** done (tenant isolation pending commit)
- [x] `register` / `login` / `refresh` with single-use refresh-token rotation stored in DB; BCrypt; required `JWT_SECRET`
- [x] RBAC rule for `/v1/admin/**`; `SecurityIT`, `AuthServiceTest`, `JwtServiceTest`, `JwtAuthenticationFilterTest`
- [x] Tenant isolation on log APIs incl. cache keys, with HTTP ITs (**uncommitted**)
- [x] `docs/security/threat-model.md`; ADR 0003; README security model

**API maturity (old Week 4):** done except Postman
- [x] `openapi.yaml`; `IdempotencyFilter` + `IdempotencyKeyEntity` + ITs; `docs/api/deprecation-plan.md`; ADR 0004; README API section

**Caching and limits (old Week 5):** partially done
- [x] Redis in Compose; cache-aside on `getLogById` with cache ITs; per-IP `RateLimitFilter` (in-memory Bucket4j) + test; ADR 0005

**Correction to the old checklist:** the old plan ticked items that do not exist: `docs/perf/n-plus-one-*.txt`, Hibernate `batch_size=20` config, a transaction-rollback test, an optimistic-lock-conflict test, and a README JPA section beyond one paragraph. They are now open items in Phase 1.

**Fixed 2026-11-13 (real-app startup):** the app did not start via Compose: Dockerfiles needed a host-built JAR, the Alpine Temurin images have no ARM build (the cause of the `linux/amd64` pin), `DATABASE_URL` was never read by Spring, and no schema existed outside tests. Now: multi-stage Dockerfiles, Flyway `V1__init.sql`, `ddl-auto: validate` everywhere, standard `SPRING_DATASOURCE_*` env vars. Verified: health 200, register 201, login 200 against Compose Postgres.

## What gets you hired (the filter for every decision)

1. A **live, working system** you can demo in 3 minutes (URL + short video).
2. **CI green on every PR**: tests, integration tests with Testcontainers.
3. A **README that sells it**: architecture diagram, how to run, real numbers (p95, throughput), tradeoffs.
4. **Async pipeline** (Kafka) with idempotency/retries: the thing backend interviews ask about.
5. **An AI feature with evaluation**, not just an API call: RAG with citations, golden-question evals, cost and safety controls. This is what separates "AI engineer" from "called an API".
6. **You can explain it**: every ADR, every number, every failure. Interviews test understanding, not file counts.

Deliberately demoted: Kubernetes/EKS/Helm (see Parking Lot). They are useful but are DevOps depth, not what backend/AI job screens test. One solid cloud deploy beats four shallow ones.

## Rules

- Finish a phase before starting the next. A checkbox is ticked only when code, tests, and docs all exist and **the file actually exists in git**.
- Add tests at the lowest layer that proves the behavior; integration tests only when real wiring/infrastructure matters.
- Keep `apps/api` and `apps/worker` independently deployable; share only contracts under `libs/common/events/v1`.
- Learning checkpoint at the end of each milestone: explain *what problem it solves, where it lives, which test proves it, what you'd improve*. If you can't, it isn't done.
- After each milestone, write 5-10 lines in `docs/learning/` (what broke, what you learned, the tradeoff). These become your interview stories.

## Phase 1: Close the Core (target: 1 week)

Goal: everything already built is committed, proven, and automated.

- [ ] Commit tenant isolation + cache-key fix (explain in the commit why `#id` alone leaked across users)
- [x] Make the real app run via Compose (datasource config, Flyway migrations, multi-stage Dockerfiles)
- [ ] Automated smoke test that boots the Compose stack (run it in CI)
- [ ] GitHub Actions: build + unit + integration tests on every PR (Docker is available on runners)
- [ ] Produce the missing evidence: `docs/perf/n-plus-one-before.txt` / `n-plus-one-after.txt` (Hibernate SQL/statistics output, with query counts)
- [ ] Add `hibernate.jdbc.batch_size=20` and prove batching; add the missing transaction-rollback and optimistic-lock-conflict tests; README JPA section
- [ ] Redis-backed rate limiter (replace in-memory Bucket4j, or Bucket4j with Redis backend); test N requests -> 429 across two app instances
- [ ] Close threat-model items 12-14 (Redis password, restrict polymorphic typing, maxmemory + LRU)
- [ ] k6 script `docs/perf/k6-local.js` + `docs/perf/k6-local-report.md` (p95, RPS, error rate, cache hit vs miss)
- [ ] README: architecture v1, run/test instructions, security model, caching/perf section with the real numbers
- [ ] `docs/testing/how-to-run-tests.md`
- [ ] Postman/Bruno collection or curl examples under `docs/api/`

Learn: Testcontainers in CI, Bucket4j/Redis, cache invalidation, reading `EXPLAIN` and Hibernate statistics, k6 metrics (why p95 not average).

## Phase 2: Make It a Real Log Product (target: 2 weeks)

Goal: the domain is believable. Right now it stores entries; it should *search and analyze* them.

- [ ] `Project` (application) + `environment` (dev/staging/prod); logs belong to a project; authorization at project level
- [ ] Severity normalization (TRACE..FATAL); structured JSON and raw-text ingestion
- [ ] Batch ingest endpoint (`POST /v1/projects/{id}/logs:batch`) with size limits and JDBC batching evidence
- [ ] Search: filters by service, level, time range, trace ID; **Postgres full-text search** (`tsvector` + GIN index) with `EXPLAIN` before/after
- [ ] Cursor/keyset pagination on all list endpoints (explain why offset pagination degrades)
- [ ] Error grouping by message fingerprint (the "Sentry" feature; great demo and interview story)
- [ ] ADR: data model + search strategy; update OpenAPI
- [ ] Tests: tenant/project isolation, pagination edge cases, search correctness, query-plan assertion for indexed search

Learn: relational modeling, indexes (B-tree vs GIN), keyset pagination, migrations, authorization models.

## Phase 3: Async Pipeline with Kafka (target: 3 weeks, three milestones)

Goal: the worker does real work. This is the highest-value backend interview topic.

**3a: Plumbing (week 1)**
- [ ] Kafka in Compose; `LogUploadedEvent` in `libs/common/events/v1`
- [ ] API publishes after ingest; worker consumes and writes derived data (parsed fields / fingerprints)
- [ ] Kafka Testcontainers integration test: publish -> consume -> DB row
- [ ] Contract test for event JSON compatibility

**3b: Correctness (week 2)**
- [ ] Idempotent consumer: `ProcessedEvent` with unique `eventId`; replaying the same event twice is a no-op (test)
- [ ] Retry topics + DLQ; poison-message policy; replay procedure documented
- [ ] Transactional outbox in the API (explain the dual-write problem it solves, with a failure-scenario test)

**3c: Operations (week 3)**
- [ ] Micrometer metrics: `logs_uploaded_total`, `logs_processed_total`, `kafka_consumer_errors_total`, consumer lag
- [ ] Actuator + Prometheus + Grafana in Compose; one dashboard screenshot committed
- [ ] Resilience4j (timeout/retry/circuit breaker) on one external call + `docs/failure-matrix.md`
- [ ] One simulated incident (kill the consumer, build lag) -> `docs/incidents/001-*.md` written as a real postmortem
- [ ] ADRs: event-driven architecture, resilience policy, observability stack

Learn: delivery semantics, at-least-once + idempotency, outbox, consumer groups/offsets, backpressure, RED metrics.

## Phase 4: AI Engineering (target: 3 weeks, three milestones)

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
- [ ] Compare at least two chunking or prompting variants using the eval numbers (this is the interview story)
- [ ] ADRs: LLM strategy, RAG design; README AI section with cost-per-query and latency numbers

Learn: structured outputs, embeddings, vector indexes, chunking, retrieval evaluation, prompt injection, cost control.

## Phase 5: Ship It (target: 2 weeks)

Goal: a public URL and a polished repo. One cloud path, done properly.

- [ ] Deploy API + worker + managed Postgres/Redis (+ Kafka alternative if cost-prohibitive, documented) on AWS ECS Fargate **or** a simpler platform if cost/time demands; choose in an ADR
- [ ] Terraform for the deployed pieces (VPC/RDS/ECS/ALB) with remote state; `terraform plan` output documented
- [ ] S3 presigned uploads (real) and cloud smoke test script
- [ ] Cloud k6 run + report; CloudWatch logs/alarms basics
- [ ] End-to-end happy-path test: upload -> ingest -> Kafka -> worker -> embeddings -> search -> RAG answer
- [ ] C4 context + container diagrams; `docs/runbook/production-runbook.md`; `docs/cost/aws-cost-estimate.md` with a budget alarm
- [ ] Demo seed data + 3-minute demo video/GIF in README
- [ ] README finalized: what/why, architecture, live link, numbers, tradeoffs, "what I'd do with more time"

## Job-Search Track (runs in parallel from Phase 3)

- [ ] Pin the repo; write a one-page project summary (problem, architecture, 3 hard problems solved, numbers)
- [ ] Prepare 6 interview stories from `docs/learning/` (N+1 fix, cache tenant leak, idempotent consumer, outbox, RAG eval result, incident postmortem)
- [ ] Weekly: 2-3 coding problems (arrays/hashmaps/graphs basics) and one system-design write-up (e.g. design a log pipeline, using LogLens as the example)
- [ ] CV bullets with numbers ("cut list query from N to 1 SQL statements", "p95 X ms at Y RPS", "RAG hit-rate 0.xx on golden set")
- [ ] Start applying once Phase 3 is done; do not wait for Phase 5. Update the repo as you interview.

## Parking Lot (only after Phase 5, only if you want it)

Kubernetes (kind, EKS, HPA, Helm), service mesh, multi-region DR, CLI tool, saved searches, anomaly detection, PII redaction, audit log, retention policies, API keys for machine ingestion, dependency/secret scanning (the last two are cheap and worth pulling forward into CI).

## Original 18-week plan

Replaced on 2026-11-13. Kafka (old Week 6), resilience/observability (7-8), AWS/ECS/Terraform (9-11), LLM/RAG/tracing (12-13), capstone (14) are all kept but reordered and trimmed; Kubernetes weeks 15-18 moved to the Parking Lot. Distributed tracing (OpenTelemetry) is optional polish after Phase 4.
