# ADR 0002: JPA and Persistence Strategy

## Status
Accepted

## Context
We need a consistent strategy for data persistence in the LogLens application to ensure data integrity, performance, and developer productivity. The system uses PostgreSQL as the backing store.

## Decision
We will use **Spring Data JPA** with **Hibernate** as the implementation provider.

### Why JPA?
- **Standardization**: JPA provides a standard specification for ORM in the Java ecosystem, decoupling the application from specific vendor implementations.
- **Productivity**: Drastically reduces boilerplate JDBC code for standard CRUD operations.
- **Integration**: First-class support in Spring Boot via `spring-boot-starter-data-jpa`.

### Why Hibernate?
- **Maturity**: It is the default provider in Spring Boot and the most widely used ORM in the industry.
- **Features**: Offers advanced capabilities like automatic dirty checking, first-level caching, and schema generation.
- **Ecosystem**: Extensive documentation and community support.

### Lazy vs Eager Strategy
- **Default to Lazy**: All relationships (`@OneToMany`, `@ManyToOne`, `@OneToOne`) must be configured as `FetchType.LAZY`.
- **Reasoning**: Eager fetching defaults often lead to fetching entire object graphs unnecessarily, causing memory bloat and the N+1 select problem.
- **Handling Eager Requirements**: When associated data is required, we explicitly fetch it using **JPQL `JOIN FETCH`** queries or **Entity Graphs** in the Repository layer (e.g., `LogRepository.findLogWithChunks`).

### Transaction Boundaries
- **Service Layer**: Transactions are defined at the Service layer using the `@Transactional` annotation.
- **Atomicity**: A service method represents a complete unit of work. If an unchecked exception occurs, the entire operation rolls back.
- **Read-Only Optimization**: Read operations should use `@Transactional(readOnly = true)` to allow Hibernate to skip dirty checking, improving performance.

### Locking Strategy
- **Optimistic Locking**: We use a `@Version` field on entities (e.g., `LogEntity`) to handle concurrent updates. This prevents "lost updates" by throwing an `OptimisticLockException` if the data has changed since it was read.
- **Pessimistic Locking**: Reserved only for specific high-contention critical sections where collisions are frequent, applied explicitly via `LockModeType.PESSIMISTIC_WRITE`.

## Consequences
- Developers must be careful with `LazyInitializationException` when accessing collections outside the transaction boundary.
- Performance tuning requires understanding Hibernate internals (proxies, persistence context).
- We accept the overhead of an ORM in exchange for development speed and maintainability.