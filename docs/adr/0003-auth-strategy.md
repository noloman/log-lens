# ADR 0003: Authentication Strategy

## Status
Accepted

## Context
LogLens needs to authenticate API consumers so that log data is only accessible to authorized users. The system currently consists of a stateless REST API (`apps/api`) and an internal worker service (`apps/worker`). Key requirements:

- Stateless authentication suitable for horizontal scaling
- Role-based access control (at minimum USER and ADMIN)
- Token refresh capability to avoid frequent re-authentication
- Compatible with a future mobile or SPA frontend

Alternatives considered:

| Approach | Pros | Cons |
|---|---|---|
| **Session cookies** | Simple, built-in CSRF support | Requires sticky sessions or a shared session store — conflicts with stateless goal |
| **OAuth 2.0 / OIDC (external IdP)** | Delegated identity, standards-based | Adds operational dependency; overkill when we are the only resource server |
| **JWT + Refresh Token (chosen)** | Stateless, self-contained claims, widely supported | Tokens cannot be revoked before expiry without a denylist; secret key management required |

## Decision
We use **JWT access tokens + opaque refresh tokens** with the following design:

### Token Lifecycle
1. **Register** — `POST /auth/register` creates a `UserEntity` with BCrypt-hashed password and default `Role.USER`.
2. **Login** — `POST /auth/login` verifies credentials, issues a JWT access token (1 h TTL) and a UUID refresh token (30 d TTL, stored in `refresh_tokens` table).
3. **Refresh** — `POST /auth/refresh` validates the refresh token and returns a new access token. The same refresh token is reused until expiry.
4. **Logout** — `POST /auth/logout` (planned) deletes the refresh token, preventing further refreshes.

### JWT Structure
- **Algorithm**: HMAC-SHA (symmetric), via JJWT library
- **Subject** (`sub`): User UUID
- **Custom claims**: `email`, `role`
- **Expiry** (`exp`): configurable, default 1 hour

### Request Authentication Flow
`JwtAuthenticationFilter` (extends `OncePerRequestFilter`) intercepts every request:
1. Extracts `Bearer <token>` from the `Authorization` header
2. Validates signature and expiry via `JwtService.validateToken()`
3. Sets `SecurityContextHolder` with user identity and `ROLE_<role>` authority
4. Invalid or missing tokens pass through unauthenticated (public endpoints still work)

### Authorization Rules (`SecurityConfig`)
| Path | Access |
|---|---|
| `/auth/**` | Public |
| `/health` | Public |
| `/admin/**` | `ROLE_ADMIN` |
| Everything else | Authenticated |

### Password Storage
- BCrypt via Spring Security's `BCryptPasswordEncoder` (adaptive cost factor, default 10 rounds)

### Key Management
- The HMAC signing secret is injected from the `JWT_SECRET` environment variable (`application.yml` provides a development-only fallback)
- Minimum key length: 32 bytes (enforced by JJWT's `Keys.hmacShaKeyFor`)

## Consequences

### Benefits
- **Stateless**: No server-side session store required — access tokens are validated purely by signature
- **Scalable**: Any API instance can validate a JWT independently
- **Separation of concerns**: Short-lived access tokens limit exposure; long-lived refresh tokens are revocable via DB deletion

### Trade-offs
- **No immediate revocation**: A compromised access token remains valid until it expires (mitigated by the 1-hour TTL)
- **Secret rotation complexity**: Changing the HMAC key invalidates all outstanding access tokens. A future migration to asymmetric keys (RSA/EC) would allow key rotation via JWKS
- **Single signing algorithm**: Symmetric HMAC means the API module both signs and verifies — acceptable while there is a single issuer

### Future Considerations
- Add a token denylist (e.g., Redis set of revoked `jti` claims) if immediate revocation becomes a requirement
- Migrate to asymmetric keys (RS256/ES256) if other services need to verify tokens without holding the signing key
- Implement refresh token rotation (issue a new refresh token on each refresh) to limit replay window
- Add rate limiting to auth endpoints (see [threat model](../security/threat-model.md))
