# ADR 0004: API Versioning and Deprecation Strategy

## Status
Accepted

## Context
LogLens exposes a REST API that will evolve over time — new endpoints, changed request/response shapes, and eventually removed features. We need a strategy to version the API so that:

- Clients can upgrade at their own pace without unexpected breakage
- We can evolve the API without being locked into early design mistakes
- Breaking changes are communicated clearly and predictably
- Deprecated endpoints have a defined sunset timeline

### Current State
- Endpoints are unversioned: `/logs`, `/auth/login`, `/auth/register`, `/auth/refresh`, `/health`
- The OpenAPI spec declares `version: 0.0.1` but this is not reflected in URL paths or headers
- No deprecation policy exists

### Alternatives Considered

| Approach | Pros | Cons |
|---|---|---|
| **URL path versioning** (`/v1/logs`) (chosen) | Obvious, self-documenting, easy to route and test; cached correctly by proxies | URL changes on major version; requires path management |
| **Header versioning** (`Accept: application/vnd.loglens.v1+json`) | Clean URLs, unlimited versions | Hidden from browsers and logs; harder to test; easy to forget |
| **Query parameter** (`/logs?version=1`) | Simple to add | Pollutes query string; optional params cause ambiguity; caching issues |
| **No versioning** | Zero overhead | Any breaking change breaks all clients simultaneously |

## Decision

### 1. URL Path Versioning

We prefix all API paths with `/v{MAJOR}`:

```
/v1/logs
/v1/auth/login
/v1/auth/register
/v1/auth/refresh
```

Infrastructure endpoints (`/health`, `/actuator/**`) remain unversioned — they are not part of the client contract.

#### Implementation in Spring
Each controller declares the version in its `@RequestMapping`:

```kotlin
@RestController
@RequestMapping("/v1/logs")
class LogController(...)
```

When `v2` is needed for a specific resource, a new controller class handles the new version while the `v1` controller continues to serve the old contract:

```kotlin
@RestController
@RequestMapping("/v2/logs")
class LogControllerV2(...)
```

#### Security Configuration
Update `SecurityConfig` to match versioned paths:

```kotlin
it.requestMatchers("/v1/auth/**").permitAll()
it.requestMatchers("/v1/admin/**").hasRole(Role.ADMIN.name)
```

### 2. What Constitutes a Breaking Change

A new **MAJOR** version (`/v1` → `/v2`) is required when any of the following occur:

| Change | Breaking? | Example |
|---|---|---|
| Remove an endpoint | Yes | Removing `GET /v1/logs` |
| Remove or rename a required request field | Yes | `serviceName` → `service` in `CreateLogRequest` |
| Remove or rename a response field | Yes | Removing `timestamp` from `LogResponse` |
| Change a field's type | Yes | `id` from `UUID` to `Long` |
| Change authentication scheme | Yes | JWT → API keys |
| Change success status code | Yes | `201` → `200` for `POST /logs` |
| Add a new optional request field | No | Adding optional `tags` to `CreateLogRequest` |
| Add a new response field | No | Adding `source` to `LogResponse` |
| Add a new endpoint | No | `GET /v1/logs/{id}` |
| Add a new optional query parameter | No | `?level=ERROR` on `GET /v1/logs` |

### 3. Deprecation Policy

When an API version is superseded:

| Phase | Duration | Action |
|---|---|---|
| **Active** | Ongoing | The current version receives features and fixes |
| **Deprecated** | Minimum 3 months | The old version still works but responses include a `Deprecation` header and a `Sunset` header with the removal date. No new features. Security fixes only. |
| **Sunset** | After deprecation period | The old version returns `410 Gone` with a response body directing clients to the new version |

#### Deprecation Headers (RFC 8594 / draft-ietf-httpapi-deprecation-header)
When a version enters the deprecated phase, all responses include:

```http
Deprecation: true
Sunset: Sat, 01 Sep 2026 00:00:00 GMT
Link: </v2/logs>; rel="successor-version"
```

#### Implementation
A servlet filter applied to deprecated version paths adds these headers:

```kotlin
@Component
class DeprecationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request, response, chain) {
        if (request.requestURI.startsWith("/v1/")) {
            response.setHeader("Deprecation", "true")
            response.setHeader("Sunset", "2026-09-01T00:00:00Z")
            response.setHeader("Link", "</v2/>; rel=\"successor-version\"")
        }
        chain.doFilter(request, response)
    }
}
```

### 4. OpenAPI Specification
- Each API version has its own OpenAPI document (e.g., `openapi-v1.yaml`, `openapi-v2.yaml`) or a single document with version-prefixed paths
- The `info.version` field reflects the API version: `1.0.0`, `2.0.0`
- Deprecated endpoints are marked with `deprecated: true` in the spec

### 5. Version Lifecycle Rules

- **Maximum 2 active versions** at any time. When `v3` is released, `v1` must already be sunset.
- **No backporting features** to deprecated versions. Only security patches.
- **Version in code, not config**: the version prefix is part of `@RequestMapping`, not a runtime property. This keeps routing explicit and testable.

## Consequences

### Benefits
- **Client safety**: clients pin to `/v1/` and upgrade to `/v2/` when ready
- **Explicit routing**: version is visible in URLs, logs, and metrics — easy to monitor adoption
- **Sunset clarity**: the `Deprecation` + `Sunset` headers give clients machine-readable migration timelines
- **Testability**: `v1` and `v2` controllers can be tested independently

### Trade-offs
- **Code duplication**: maintaining two controller versions means duplicating DTOs and routing logic. Mitigated by sharing the service layer — only the HTTP contract differs.
- **Path prefix overhead**: every path reference (tests, docs, security config, client SDKs) must include `/v1/`. One-time migration cost.
- **Discipline required**: team must agree on what constitutes a breaking change and resist the temptation to make breaking changes within a version.

### Migration Plan
1. Rename current paths from `/logs` → `/v1/logs`, `/auth/**` → `/v1/auth/**`
2. Update `SecurityConfig` matchers to `/v1/auth/**` and `/v1/admin/**`
3. Update OpenAPI spec paths
4. Update integration tests
5. Add a `DeprecationFilter` (inactive until `v2` is introduced)
