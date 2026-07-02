# Backend Engineering Guide

LogLens is built as a realistic backend system. The product gives the work shape, and the engineering process keeps each change understandable, testable, and reviewable.

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
7. Document the decision or tradeoff.
8. Commit the result atomically.

## Engineering Areas

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

## Definition Of Done

A topic is done when:

- The behavior works.
- The focused tests pass.
- Relevant failure modes were considered.
- The docs or roadmap explain the decision.
- The commit is small enough to review on its own.
- The code path can be explained clearly.

## Review Questions

After each topic, answer:

- What problem did this solve?
- What would break if this code were removed?
- What did the test prove?
- What did the test not prove?
- What would change in a larger production system?
- What should be revisited later?
