# Backend Engineering Learning Guide

LogLens exists to teach backend engineering through a realistic product. The product gives the work shape, but the learning goal is the priority.

## Working Principle

Build one backend capability at a time:

```text
concept -> implementation -> tests -> failure analysis -> documentation -> commit
```

Do not treat green tests as the only goal. A change is valuable when it leaves behind working behavior, a clear test signal, and an explanation that can be revisited later.

## How To Work Through A Topic

For each roadmap topic:

1. Name the backend concept.
2. Find the current code path.
3. Make the smallest useful change.
4. Run a focused test.
5. Read the failure carefully when it fails.
6. Fix the cause, not only the symptom.
7. Document the decision or lesson.
8. Commit the result atomically.

## What To Learn By Area

### HTTP APIs

- How requests reach controllers.
- How request DTOs are validated.
- How response status codes and bodies are shaped.
- How errors become RFC7807 `ProblemDetail` responses.

### Persistence

- How entities differ from DTOs.
- How repository methods become SQL queries.
- How transactions define consistency boundaries.
- How lazy loading, fetch joins, batching, and indexes affect performance.

### Security

- How authentication establishes the current user.
- How authorization restricts actions.
- How tenant isolation prevents data leaks.
- How refresh tokens, password hashing, and generic errors reduce risk.

### Distributed Systems

- How services communicate asynchronously.
- How idempotency prevents duplicate side effects.
- How retries, dead-letter queues, and replay affect correctness.

### Operations

- How logs, metrics, traces, and request IDs support debugging.
- How health checks, readiness, runbooks, and incident notes support production.
- How load tests expose bottlenecks before deployment.

## Definition Of Done For Learning

A topic is done when:

- The behavior works.
- The focused tests pass.
- The relevant failure modes were considered.
- The docs or roadmap explain the decision.
- The commit is small enough to review on its own.
- The learner can explain the code path without reading every line aloud.

## Suggested Reflection Questions

After each topic, answer:

- What problem did this solve?
- What would break if this code were removed?
- What did the test prove?
- What did the test not prove?
- What would change in a larger production system?
- What should be revisited later?
