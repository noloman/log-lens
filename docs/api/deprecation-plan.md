# API Deprecation Plan

This document defines how LogLens communicates, manages, and executes the deprecation and removal of API versions and individual endpoints. It complements [ADR 0004: API Versioning and Deprecation Strategy](../adr/0004-versioning-strategy.md).

## Current API Versions

| Version | Base Path | Status | Sunset Date |
|---|---|---|---|
| v1 | `/v1/` | **Active** | — |

## Lifecycle Phases

Every API version or endpoint moves through three phases:

```
Active ──► Deprecated ──► Sunset
                │              │
          Still works,     Returns 410 Gone.
          headers warn.    Code removed in
          Min 3 months.    next release.
```

### Phase 1: Active
- Receives new features, bug fixes, and security patches.
- No deprecation signals in responses.

### Phase 2: Deprecated
- Triggered when a successor version (`/v2/`) is released.
- **Minimum duration: 3 months** from the announcement date.
- The deprecated version continues to function identically.
- All responses include deprecation headers (see below).
- No new features are backported. Only security fixes.
- The OpenAPI spec marks deprecated endpoints with `deprecated: true`.

### Phase 3: Sunset
- The deprecated version stops serving requests.
- All endpoints under the sunset version return `410 Gone`:
  ```json
  {
    "error": "Gone",
    "message": "API v1 has been sunset. Migrate to /v2/. See https://docs.loglens.dev/migration/v1-to-v2"
  }
  ```
- Controller code for the sunset version is removed in the following release.

## Deprecation Headers

When a version enters the Deprecated phase, a `DeprecationFilter` adds these headers to every response (per [RFC 8594](https://www.rfc-editor.org/rfc/rfc8594) and the [Sunset header draft](https://www.rfc-editor.org/rfc/rfc8594)):

```http
Deprecation: true
Sunset: Sun, 01 Jun 2026 00:00:00 GMT
Link: </v2/logs>; rel="successor-version"
```

| Header | Purpose |
|---|---|
| `Deprecation` | Machine-readable signal that this version is deprecated |
| `Sunset` | The date after which this version will stop working |
| `Link` | Points the client to the replacement version |

Clients that monitor response headers can detect deprecation automatically and alert their teams before the sunset date.

## Deprecation of Individual Endpoints

Not every deprecation requires a full version bump. Individual endpoints within an active version can be deprecated when:

- A better endpoint replaces it within the same version (e.g., `GET /v1/logs/search` replaces a query parameter pattern)
- An endpoint was added experimentally and is being removed

For individual endpoint deprecation:

1. Add `deprecated: true` to the endpoint in the OpenAPI spec.
2. Add `@Deprecated` to the controller method.
3. Return the `Deprecation` and `Sunset` headers for that specific endpoint.
4. After the sunset period, the endpoint returns `410 Gone` while the rest of the version remains active.

## Communication Checklist

When deprecating a version or endpoint, complete the following:

- [ ] **Announce**: Post a deprecation notice in the changelog and API documentation
- [ ] **Headers**: Deploy the `DeprecationFilter` with the correct `Sunset` date
- [ ] **OpenAPI**: Mark affected endpoints with `deprecated: true`
- [ ] **Migration guide**: Publish `docs/api/migration-v{N}-to-v{N+1}.md` with:
  - Mapping of old endpoints to new endpoints
  - Request/response schema diffs
  - Code examples in common languages
- [ ] **Monitoring**: Add metrics to track request volume on the deprecated version
- [ ] **Client notification**: If known consumers exist, notify them directly
- [ ] **Sunset**: After the minimum 3-month period and <5% traffic remains, proceed with sunset

## Monitoring Deprecated Versions

Track adoption and migration progress with these metrics:

| Metric | Purpose |
|---|---|
| `http_requests_total{version="v1"}` | Volume on the deprecated version |
| `http_requests_total{version="v2"}` | Volume on the successor version |
| `v1_percentage = v1 / (v1 + v2) * 100` | Migration progress — sunset when <5% |

These can be derived from access logs or a Micrometer counter incremented by the `DeprecationFilter`.

## Constraints

- **Maximum 2 active versions** at any time. A third version cannot be released until the oldest is sunset.
- **Minimum 3-month deprecation window**. No exceptions for public-facing versions.
- **No breaking changes within a version**. If a change is breaking, it must go in the next version.
- **Security fixes are always backported** to deprecated (but not sunset) versions.

## Example Timeline

Hypothetical deprecation of v1 when v2 is introduced:

| Date | Action |
|---|---|
| 2026-06-01 | `v2` released. `v1` enters Deprecated phase. `Sunset` header set to `2026-09-01`. |
| 2026-06-01 | Migration guide published at `docs/api/migration-v1-to-v2.md`. |
| 2026-06-15 | Dashboard shows 60% of traffic still on `v1`. |
| 2026-08-01 | Traffic on `v1` drops to 10%. Reminder sent to remaining consumers. |
| 2026-09-01 | `v1` enters Sunset phase. All `v1` endpoints return `410 Gone`. |
| 2026-10-01 | `v1` controller code removed from the codebase. |

## Related Documents

- [ADR 0004: API Versioning and Deprecation Strategy](../adr/0004-versioning-strategy.md) — rationale and design decisions
- [OpenAPI Specification](../../OpenAPI.yaml) — current API contract
- [Threat Model](../security/threat-model.md) — security considerations for versioned endpoints
