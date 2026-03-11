# ADR 0005: Redis Caching Strategy

## Status
Accepted

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
- `POST /v1/logs` — `@CacheEvict(value = "logs", allEntries = true)` clears the entire cache on writes
- Logs are immutable after creation, so there are no update/delete paths to handle
- `allEntries = true` is deliberately coarse: the new log has no existing cache entry to evict, and this strategy also correctly invalidates list results if they are cached in the future
- TTL provides a safety net: even without explicit eviction, stale entries expire after 10 minutes

### 4. Serialization
Cache values are serialized as JSON using `GenericJackson2JsonRedisSerializer` with a custom `ObjectMapper`. This keeps cached data human-readable and debuggable via `redis-cli`.

The custom `ObjectMapper` registers three critical modules:

- **`JavaTimeModule`** — required to serialize `java.time.Instant` (`LogResponse.timestamp`). Without it, Jackson throws `java.time.Instant not supported by default`.
- **`KotlinModule`** — required to *deserialize* Kotlin data classes. Data classes have no no-arg constructor; `KotlinModule` teaches Jackson to call the primary constructor with named parameters. Without it, deserialization fails with "no Creators exist."
- **`activateDefaultTyping(EVERYTHING)`** — writes type information into the JSON (e.g., `["me.manulorenzo.loglens.api.dto.LogResponse", {...}]`). Without it, `GenericJackson2JsonRedisSerializer` cannot determine the target class on deserialization and returns a `LinkedHashMap` instead of a typed object.

This `ObjectMapper` is separate from Spring Boot's auto-configured one — Spring's global `ObjectMapper` is only used for HTTP serialization, not for Redis cache serialization.

### 5. Conditional Activation
`CacheConfig` is annotated with `@ConditionalOnProperty(name = ["spring.data.redis.host"])`. If the property is absent, no `CacheManager` bean is created and the app works without Redis.

This was chosen over `@ConditionalOnBean(RedisConnectionFactory::class)` because of a Spring bean ordering issue: `@ConditionalOnBean` evaluates before auto-configuration runs, so it would always see "no bean" and never activate. Property-based conditions evaluate immediately with no ordering dependency.

### 6. Infrastructure
- **Local dev**: Redis 7 Alpine container in `docker-compose.local.yml` with healthcheck (`redis-cli ping`). The API container receives `SPRING_DATA_REDIS_HOST: redis` via environment and `depends_on` with `condition: service_healthy`.
- **Tests**: Redis and Cache auto-configuration are excluded globally in `src/test/resources/application.yml` to prevent non-cache tests from requiring a Redis connection. Cache-specific tests (`LogServiceCacheIT`) opt back in via `@SpringBootTest(properties = ["spring.autoconfigure.exclude="])` and provide a Redis container through Testcontainers `GenericContainer("redis:7-alpine")` with `@DynamicPropertySource`.
- **Production**: Managed Redis (e.g., ElastiCache, Cloud Memorystore) — configured via `SPRING_DATA_REDIS_HOST` environment variable.

### 7. Configuration
```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379
```

Per-cache TTL is configured programmatically in `CacheConfig` via `RedisCacheManager.builder()` with `cacheDefaults()`, not via global properties, to allow different TTLs per cache name in the future.

## Consequences

### Benefits
- **Reduced DB load**: repeated reads for the same log entry are served from Redis
- **Lower latency**: Redis lookups are sub-millisecond vs. multi-millisecond DB queries
- **Horizontal scaling**: shared cache means adding API replicas doesn't multiply DB read load
- **Simple integration**: Spring's annotation-driven caching requires minimal code changes
- **Graceful degradation**: `@ConditionalOnProperty` means the app starts without Redis if the property is unset

### Trade-offs
- **New dependency**: Redis must be available in all environments where caching is enabled (dev, test, prod)
- **Cache consistency**: there is a brief window (up to TTL) where a cache entry could be stale if eviction fails or a write bypasses the service layer
- **Serialization cost**: JSON serialization/deserialization adds minor overhead per cache miss; the custom `ObjectMapper` with type info increases payload size slightly
- **Test isolation**: the auto-config exclusion pattern adds complexity — every new `@SpringBootTest` that needs Redis must explicitly opt back in
- **Monitoring**: need to track cache hit/miss rates via Actuator or Redis `INFO` stats to validate the strategy is effective
