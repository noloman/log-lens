# ADR 0005: Redis Caching Strategy

## Status
Proposed

## Context
LogLens log queries hit the database on every request. As log volume grows, read-heavy endpoints like `GET /v1/logs/{id}` will create unnecessary DB load for data that is immutable after creation. We need a caching layer to reduce latency and DB pressure without adding significant operational complexity.

### Alternatives Considered

| Approach | Pros | Cons |
|---|---|---|
| **Redis via Spring Cache abstraction** (chosen) | Mature Spring integration (`@Cacheable`/`@CacheEvict`); shared cache across API instances; rich ecosystem; TTL support | New infrastructure dependency (Redis); network hop for cache reads |
| **Caffeine (in-process cache)** | Zero infrastructure; nanosecond lookups; no serialization | Not shared across instances; each replica has its own cache; cold starts after redeploy |
| **No caching** | Zero complexity | DB hit on every read; latency grows with data volume |

## Decision

### 1. Technology
Use Redis as the caching backend via `spring-boot-starter-data-redis` and Spring's `@EnableCaching` abstraction. Redis is chosen over in-process caching because LogLens will run multiple API replicas and a shared cache avoids redundant DB queries across instances.

### 2. Cache Targets

| Endpoint | Cache Name | Key | TTL |
|---|---|---|---|
| `GET /v1/logs/{id}` | `logs` | `logs::{id}` | 10 minutes |

The list endpoint (`GET /v1/logs`) is **not cached** initially — it supports filtering and pagination, making cache key cardinality high and hit rates low.

### 3. Invalidation Strategy
- `POST /v1/logs` — `@CacheEvict(value = "logs", allEntries = true)` clears the cache on writes
- Logs are immutable after creation, so there are no update/delete paths to handle
- TTL provides a safety net: even without explicit eviction, stale entries expire

### 4. Serialization
Cache values are serialized as JSON using `GenericJackson2JsonRedisSerializer`. This keeps cached data human-readable and debuggable via `redis-cli`.

### 5. Infrastructure
- **Local dev**: Redis container in `docker-compose.local.yml`
- **Tests**: Testcontainers `GenericContainer("redis:7-alpine")` with `@DynamicPropertySource`
- **Production**: Managed Redis (e.g., ElastiCache, Cloud Memorystore) — configured via `SPRING_DATA_REDIS_HOST` environment variable

### 6. Configuration
```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379
```

Per-cache TTL is configured programmatically in `CacheConfig` via `RedisCacheManager.builder()` with `withInitialCacheConfigurations()`, not via global properties, to allow different TTLs per cache name in the future.

## Consequences

### Benefits
- **Reduced DB load**: repeated reads for the same log entry are served from Redis
- **Lower latency**: Redis lookups are sub-millisecond vs. multi-millisecond DB queries
- **Horizontal scaling**: shared cache means adding API replicas doesn't multiply DB read load
- **Simple integration**: Spring's annotation-driven caching requires minimal code changes

### Trade-offs
- **New dependency**: Redis must be available in all environments (dev, test, prod)
- **Cache consistency**: there is a brief window (up to TTL) where a cache entry could be stale if eviction fails or a write bypasses the service layer
- **Serialization cost**: JSON serialization/deserialization adds minor overhead per cache miss
- **Monitoring**: need to track cache hit/miss rates via Actuator or Redis `INFO` stats to validate the strategy is effective
