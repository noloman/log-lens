# Testing Strategy

This document defines the baseline test layers for LogLens. Keep tests close to the behavior they prove, and prefer the narrowest Spring test slice that still exercises the relevant framework wiring.

## Test Layers

- Unit tests cover pure service logic with mocked collaborators.
- MVC slice tests use `@WebMvcTest` and `MockMvc` for controller request/response behavior, validation, and serialization.
- Integration tests use `@SpringBootTest` when behavior depends on the full Spring context, persistence, cache wiring, security filters, or Testcontainers.
- Repository and persistence tests should use real Postgres through Testcontainers when SQL behavior, JPA mappings, transactions, or query methods matter.

## Naming

- Use `*Test` for unit and slice tests that do not require external containers.
- Use `*IT` for integration tests that start the full app context, a random web port, or Testcontainers-backed infrastructure.
- Test names should describe externally visible behavior, for example `POST logs with invalid data should fail`.

## API And Validation Tests

- Controller tests should assert HTTP status codes and response JSON shape.
- Validation tests should send invalid JSON through `MockMvc`, not instantiate DTOs directly.
- API error responses should use Spring RFC7807 `ProblemDetail` responses, with `application/problem+json` where Spring provides it.
- Security-dependent MVC tests with filters disabled may attach a `Principal` directly to the request when the controller accepts `Authentication`.

## Running Focused Tests

Run a single test class while iterating:

```bash
./gradlew :apps:api:test --tests 'me.manulorenzo.loglens.api.controller.LogControllerTest'
```

Run focused groups before committing related API changes:

```bash
./gradlew :apps:api:test \
  --tests 'me.manulorenzo.loglens.api.controller.LogControllerTest' \
  --tests 'me.manulorenzo.loglens.api.service.LogServiceCacheIT'
```

Run the full suite when Docker/Testcontainers are available:

```bash
./gradlew test
```
