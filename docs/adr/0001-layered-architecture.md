# ADR 0001: Layered Architecture for Services

## Status
Accepted

## Context
We need a clear structure for maintainable Kotlin/Spring services, enabling testability and future evolution (Kafka, Redis, AWS, etc).

## Decision
Use a layered architecture per service:
- api (controllers)
- service (business logic)
- repository (persistence)
- config (wiring)
- observability (logging/metrics/tracing)

## Consequences
- Clear boundaries for unit + integration tests
- Easier refactors toward hexagonal if needed
- Consistent structure across API and Worker

### Trade-offs
- Risk of "Anemic Domain Model" if business logic stays entirely in the Service layer
- Boilerplate code required even for simple CRUD operations (pass-through methods)

## Compliance
- Use ArchUnit tests to enforce layer boundaries (e.g., `api` package cannot import `repository` package directly)