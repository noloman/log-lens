# LogLens Threat Model

## Scope

This document covers the **API module** (`apps/api`) of LogLens, focusing on authentication, authorization, and data-flow threats. The worker module communicates internally and is out of scope for now.

## Assets

| Asset | Sensitivity | Storage |
|---|---|---|
| User credentials (email + password hash) | High | `users` table, BCrypt-hashed |
| JWT signing secret | Critical | `JWT_SECRET` env var, injected via `application.yml` |
| Access tokens (JWT) | High | Client-side (Authorization header) |
| Refresh tokens (UUID) | High | `refresh_tokens` table, client receives opaque UUID |
| Log data (file uploads) | Medium | `logs` / `log_chunks` tables |
| Log data (structured entries) | Medium | `log_entries` table |
| Cached log responses | Medium | Redis (JSON-serialized `LogResponse` with type metadata) |

## Trust Boundaries

```
                       ┌───────────────────────────────────────────┐
  Internet             │            API Module (8080)               │
 ─────────────────────►│                                            │
  (untrusted)          │  ┌──────────┐  ┌────────────┐  ┌───────┐ │     ┌────────────┐
                       │  │ Rate     │─►│ Security   │─►│ Ctrl  │─┼────►│ PostgreSQL │
                       │  │ Limit    │  │ Filter     │  │ + Svc │ │     └────────────┘
                       │  │ Filter   │  │ Chain      │  └───┬───┘ │
                       │  └──────────┘  └────────────┘      │     │     ┌────────────┐
                       │                                    └─────┼────►│   Redis    │
                       │                                          │     └────────────┘
                       └───────────────────────────────────────────┘
```

**Boundary 1 — Internet to API**: All HTTP requests cross this boundary. The `RateLimitFilter` enforces per-IP rate limits (100 req/min) before requests reach the security filter chain.
**Boundary 2 — API to Database**: JDBC over a private network. Parameterized queries via JPA prevent SQL injection.
**Boundary 3 — API to Redis**: Lettuce client over a private network. Used for cache reads/writes (`@Cacheable`/`@CacheEvict`). No authentication configured by default.

## STRIDE Analysis

### 1. Spoofing

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **S-1**: Attacker obtains a valid JWT and impersonates a user | JWTs are signed with HMAC-SHA and expire after 1 hour (`jwt.expiration-ms: 3600000`) | Token theft via XSS or network sniffing if HTTPS is not enforced |
| **S-2**: Brute-force login attempts | BCrypt hashing makes offline attacks expensive; `RateLimitFilter` limits all endpoints to 100 req/min per IP (Bucket4j token bucket) | Rate limit is per-IP, not per-user — distributed attacks from many IPs bypass it. Limit is global, not tuned specifically for auth endpoints |
| **S-3**: Forged JWT with manipulated claims (e.g., role escalation) | `jwtService.validateToken` verifies the HMAC signature; tampered tokens are rejected | None if the signing key remains secret |

### 2. Tampering

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **T-1**: Modify JWT payload to escalate role | HMAC signature verification rejects any modifications | None while key is kept secret |
| **T-2**: SQL injection through auth or log endpoints | Spring Data JPA uses parameterized queries exclusively (including `LogEntryRepository.findByServiceName`) | None for current query patterns |
| **T-3**: Modify refresh token UUID to access another user's session | Refresh tokens are random UUIDs (122 bits of entropy), looked up by exact match | Negligible collision probability |
| **T-4**: Inject malicious content via log entry fields (`serviceName`, `level`, `message`) | Fields are stored as plain strings via JPA parameterized inserts — no injection at the DB level | If log data is rendered in a UI without escaping, stored XSS is possible |
| **T-5**: Poison Redis cache to serve tampered log entries | Redis is on a private network; no public access | No Redis authentication configured — an attacker with network access to the Redis instance can write arbitrary cache entries. `activateDefaultTyping(EVERYTHING)` in the serializer could enable deserialization attacks if the attacker can inject crafted type metadata |

### 3. Repudiation

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **R-1**: User denies performing an action | `CorrelationIdFilter` propagates `X-Request-Id` via MDC for log correlation | No audit log that links actions to authenticated user IDs |

### 4. Information Disclosure

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **I-1**: Credential leak via error responses | `GlobalExceptionHandler` returns generic messages; password hashes are never serialized | Generic `Exception` handler exposes `ex.message` which may leak internal details |
| **I-4**: Unauthorized access to log entries via `GET /logs` or `GET /logs/{id}` | Endpoints require authentication (JWT); unauthenticated requests are rejected by the security filter chain | No tenant/ownership isolation — any authenticated user can read all log entries. Cached responses in Redis contain full `LogResponse` JSON including type metadata |
| **I-2**: JWT secret exposure | Secret is injected via environment variable, not hardcoded in production | Default fallback value in `application.yml` could be deployed accidentally |
| **I-3**: Timing attack on password comparison | BCrypt's `matches()` is constant-time by design | None |

### 5. Denial of Service

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **D-1**: Flood `/auth/register` to fill the `users` table | `RateLimitFilter` limits to 100 req/min per IP | Distributed attacks from many IPs can still create accounts at scale; no CAPTCHA or email verification |
| **D-2**: Flood `/auth/login` with invalid credentials | BCrypt is intentionally slow, which amplifies CPU load under attack; `RateLimitFilter` limits to 100 req/min per IP | Rate limit is global (not auth-specific) — a stricter limit for auth endpoints would reduce exposure further |
| **D-3**: Accumulate refresh tokens by repeated logins | No token-per-user limit | `refresh_tokens` table grows unbounded per user |
| **D-4**: Flood `POST /logs` to fill the `log_entries` table | Endpoint requires authentication; `RateLimitFilter` limits to 100 req/min per IP | An authenticated user can still insert ~100 rows/min; no payload size limit |
| **D-5**: Exhaust Redis memory with cache entries | TTL of 10 minutes auto-expires entries; `@CacheEvict(allEntries = true)` on writes clears the cache | No `maxmemory` policy configured on Redis — unbounded cache growth possible under high-cardinality reads. No eviction policy (e.g., `allkeys-lru`) configured |

### 6. Elevation of Privilege

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **E-1**: User accesses `/admin/**` endpoints | Spring Security requires `ROLE_ADMIN`; role is embedded in JWT at issuance time | If a USER account's role is upgraded in the DB, existing JWTs still carry the old role (delay until token expiry) |
| **E-2**: Self-registration with ADMIN role | `AuthService.register()` hardcodes `Role.USER` via `UserEntity` default | None — role parameter is not exposed in `AuthRequest` |

## Resolved Issues

| # | Issue | Resolution |
|---|---|---|
| 1 | No rate limiting on auth endpoints | `RateLimitFilter` (Bucket4j) applies 100 req/min per IP to all endpoints including `/v1/auth/**` |
| 10 | No rate limiting on `POST /logs` | Covered by the same global `RateLimitFilter` |

## Open Issues (Ordered by Priority)

| # | Issue | Severity | Suggested Mitigation |
|---|---|---|---|
| 2 | Generic exception handler may leak internal details | Medium | Replace catch-all handler body with a static message |
| 3 | No audit logging for auth events | Medium | Log login, register, refresh, and logout events with user ID |
| 4 | Refresh tokens are not rotated on use | Medium | Issue a new refresh token on each `/auth/refresh` call and revoke the old one |
| 8 | No tenant isolation on log entries | Medium | Filter `GET /logs` by the authenticated user's ID; add a `userId` column to `log_entries` |
| 12 | No Redis authentication | Medium | Configure `requirepass` on Redis and set `spring.data.redis.password`; critical for non-private network deployments |
| 13 | Redis `activateDefaultTyping(EVERYTHING)` deserialization risk | Medium | Restrict `BasicPolymorphicTypeValidator` to allow only `me.manulorenzo.loglens.api.dto` package instead of `Any::class.java` |
| 14 | No Redis `maxmemory` or eviction policy | Medium | Configure `maxmemory` and `maxmemory-policy allkeys-lru` on Redis to prevent unbounded memory growth |
| 15 | Rate limiting is per-IP, not per-user | Medium | Add user-based rate limiting (from JWT `sub` claim) for authenticated endpoints; IP-based limits are unfair for shared IPs and ineffective against distributed attacks |
| 16 | Rate limiting is in-memory, not distributed | Medium | If running multiple API replicas, switch `ConcurrentHashMap` to Bucket4j's Redis proxy (`bucket4j_jdk17-lettuce`) so rate limits are shared across instances |
| 5 | No per-user limit on active refresh tokens | Low | Cap active tokens per user and delete oldest on overflow |
| 6 | Default JWT secret fallback in `application.yml` | Low | Fail startup if `JWT_SECRET` env var is missing (remove default) |
| 7 | HTTPS not enforced at the application level | Low | Enforce via reverse proxy or Spring's `requiresSecure()` |
| 9 | No input validation on log level values | Low | Validate `level` against an enum allowlist (e.g., TRACE, DEBUG, INFO, WARN, ERROR) |
| 11 | Stored XSS risk if log entries are rendered in a UI | Low | Sanitize or escape log entry fields before rendering |
