package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.dto.CreateLogRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    properties = [
        // Re-enable Redis and Cache auto-configuration excluded in test application.yml
        "spring.autoconfigure.exclude=",
    ],
)
@Testcontainers
class LogServiceCacheIT {
    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }

    @Autowired
    lateinit var service: LogEntryService

    @Autowired
    lateinit var cacheManager: CacheManager

    @Test
    fun `getLogById returns cached result on second call`() {
        val log = service.createLog(CreateLogRequest("svc", "INFO", "test"))

        service.getLogById(log.id) // populates cache

        val cache = cacheManager.getCache("logs")!!
        assertThat(cache.get(log.id)).isNotNull

        service.getLogById(log.id) // should hit cache, not DB
    }

    @Test
    fun `createLog evicts cache`() {
        val log = service.createLog(CreateLogRequest("svc", "INFO", "test"))
        service.getLogById(log.id) // populate cache

        service.createLog(CreateLogRequest("svc", "INFO", "new")) // should evict

        val cache = cacheManager.getCache("logs")!!
        assertThat(cache.get(log.id)).isNull()
    }
}
