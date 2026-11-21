package me.manulorenzo.loglens.api.service

import jakarta.persistence.EntityNotFoundException
import me.manulorenzo.loglens.api.dto.CreateLogRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(
    properties = [
        // Re-enable Redis and Cache autoconfiguration excluded in test application.yml
        "spring.autoconfigure.exclude=",
    ],
)
@Testcontainers
class LogServiceCacheIT {
    companion object {
        private const val redisPassword = "cache-test-password"

        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7-alpine")
            .withCommand("redis-server", "--requirepass", redisPassword)
            .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
            registry.add("spring.data.redis.password") { redisPassword }
        }
    }

    @Autowired
    lateinit var service: LogEntryService

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun `getLogById returns cached result on second call`() {
        val userId = UUID.randomUUID()
        val log = service.createLog(userId, CreateLogRequest("svc", "INFO", "test"))

        service.getLogById(log.id, userId) // populates cache

        val cache = cacheManager.getCache("logs")!!
        assertThat(cache.get("$userId:${log.id}")).isNotNull
        assertThat(redisTemplate.opsForValue().get("logs::$userId:${log.id}"))
            .contains("\"serviceName\"")
            .doesNotContain("@class")

        service.getLogById(log.id, userId) // should hit cache, not DB
    }

    @Test
    fun `createLog evicts cache`() {
        val userId = UUID.randomUUID()
        val log = service.createLog(userId, CreateLogRequest("svc", "INFO", "test"))

        service.getLogById(log.id, userId) // populate cache

        service.createLog(userId, CreateLogRequest("svc", "INFO", "new")) // should evict

        val cache = cacheManager.getCache("logs")!!
        assertThat(cache.get("$userId:${log.id}")).isNull()
    }

    @Test
    fun `cached log remains isolated from another user`() {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val log = service.createLog(ownerId, CreateLogRequest("svc", "INFO", "owner only"))

        service.getLogById(log.id, ownerId)

        assertThrows<EntityNotFoundException> {
            service.getLogById(log.id, otherUserId)
        }
    }
}
