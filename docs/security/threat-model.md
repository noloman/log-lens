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
| Log data | Medium | `logs` / `log_chunks` tables |

## Trust Boundaries

```
                       ┌──────────────────────────────────┐
  Internet             │         API Module (8080)         │
 ─────────────────────►│                                   │
  (untrusted)          │  ┌────────────┐  ┌────────────┐  │     ┌────────────┐
                       │  │ Security   │─►│ Controller │──┼────►│ PostgreSQL │
                       │  │ Filter     │  │ + Service  │  │     └────────────┘
                       │  │ Chain      │  └────────────┘  │
                       │  └────────────┘                  │
                       └──────────────────────────────────┘
```

**Boundary 1 — Internet to API**: All HTTP requests cross this boundary. The JWT filter validates tokens here.
**Boundary 2 — API to Database**: JDBC over a private network. Parameterized queries via JPA prevent SQL injection.

## STRIDE Analysis

### 1. Spoofing

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **S-1**: Attacker obtains a valid JWT and impersonates a user | JWTs are signed with HMAC-SHA and expire after 1 hour (`jwt.expiration-ms: 3600000`) | Token theft via XSS or network sniffing if HTTPS is not enforced |
| **S-2**: Brute-force login attempts | BCrypt hashing makes offline attacks expensive | No rate limiting on `/auth/login` — online brute-force is possible |
| **S-3**: Forged JWT with manipulated claims (e.g., role escalation) | `jwtService.validateToken` verifies the HMAC signature; tampered tokens are rejected | None if the signing key remains secret |

### 2. Tampering

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **T-1**: Modify JWT payload to escalate role | HMAC signature verification rejects any modifications | None while key is kept secret |
| **T-2**: SQL injection through auth endpoints | Spring Data JPA uses parameterized queries exclusively | None for current query patterns |
| **T-3**: Modify refresh token UUID to access another user's session | Refresh tokens are random UUIDs (122 bits of entropy), looked up by exact match | Negligible collision probability |

### 3. Repudiation

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **R-1**: User denies performing an action | `CorrelationIdFilter` propagates `X-Request-Id` via MDC for log correlation | No audit log that links actions to authenticated user IDs |

### 4. Information Disclosure

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **I-1**: Credential leak via error responses | `GlobalExceptionHandler` returns generic messages; password hashes are never serialized | Generic `Exception` handler exposes `ex.message` which may leak internal details |
| **I-2**: JWT secret exposure | Secret is injected via environment variable, not hardcoded in production | Default fallback value in `application.yml` could be deployed accidentally |
| **I-3**: Timing attack on password comparison | BCrypt's `matches()` is constant-time by design | None |

### 5. Denial of Service

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **D-1**: Flood `/auth/register` to fill the `users` table | None | No rate limiting; attacker can create unlimited accounts |
| **D-2**: Flood `/auth/login` with invalid credentials | BCrypt is intentionally slow, which amplifies CPU load under attack | No rate limiting — high request volume could exhaust server threads |
| **D-3**: Accumulate refresh tokens by repeated logins | No token-per-user limit | `refresh_tokens` table grows unbounded per user |

### 6. Elevation of Privilege

| Threat | Current Mitigation | Residual Risk |
|---|---|---|
| **E-1**: User accesses `/admin/**` endpoints | Spring Security requires `ROLE_ADMIN`; role is embedded in JWT at issuance time | If a USER account's role is upgraded in the DB, existing JWTs still carry the old role (delay until token expiry) |
| **E-2**: Self-registration with ADMIN role | `AuthService.register()` hardcodes `Role.USER` via `UserEntity` default | None — role parameter is not exposed in `AuthRequest` |

## Open Issues (Ordered by Priority)

| # | Issue | Severity | Suggested Mitigation |
|---|---|---|---|
| 1 | No rate limiting on auth endpoints | High | Add a rate-limiting filter (e.g., Bucket4j or Spring Cloud Gateway) |
| 2 | Generic exception handler may leak internal details | Medium | Replace catch-all handler body with a static message |
| 3 | No audit logging for auth events | Medium | Log login, register, refresh, and logout events with user ID |
| 4 | Refresh tokens are not rotated on use | Medium | Issue a new refresh token on each `/auth/refresh` call and revoke the old one |
| 5 | No per-user limit on active refresh tokens | Low | Cap active tokens per user and delete oldest on overflow |
| 6 | Default JWT secret fallback in `application.yml` | Low | Fail startup if `JWT_SECRET` env var is missing (remove default) |
| 7 | HTTPS not enforced at the application level | Low | Enforce via reverse proxy or Spring's `requiresSecure()` |
