package me.manulorenzo.loglens.api.service

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LogService::class)
class LogServiceIT {
    companion object {
        @Container
        @JvmField
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            // Enable Hibernate statistics for query counting
            registry.add("spring.jpa.properties.hibernate.generate_statistics") { "true" }
        }
    }

    @Autowired
    private lateinit var logService: LogService

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var statistics: Statistics

    @BeforeEach
    fun setUp() {
        // Get the Hibernate session factory and its statistics object
        val sessionFactory = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java)
        statistics = sessionFactory.statistics
        statistics.isStatisticsEnabled = true
        statistics.clear() // Clear stats before each test
    }

    @Test
    fun `getLogWithChunksNaive demonstrates N+1 problem`() {
        // --- Given ---
        val logId = setupLogWithChunks(chunkCount = 10)
        entityManager.flush() // Ensure data is written to DB before clearing context
        entityManager.clear() // Clear persistence context to ensure we hit the DB
        statistics.clear() // Reset stats after setup

        // --- When ---
        logService.getLogWithChunksNaive(logId)

        // --- Then ---
        // Expect 1 query for the LogEntity + 1 query for the chunks collection (lazy loaded)
        assertThat(statistics.prepareStatementCount).isEqualTo(2)
    }

    @Test
    fun `getLogWithChunksOptimized fetches in a single query`() {
        // --- Given ---
        val logId = setupLogWithChunks(chunkCount = 10)
        entityManager.flush() // Ensure data is written to DB before clearing context
        entityManager.clear() // Clear persistence context to ensure we hit the DB
        statistics.clear() // Reset stats after setup

        // --- When ---
        logService.getLogWithChunksOptimized(logId)

        // --- Then ---
        // Expect 1 query with JOIN FETCH for both LogEntity and its chunks
        assertThat(statistics.prepareStatementCount).isEqualTo(1)
    }

    private fun setupLogWithChunks(chunkCount: Int): UUID {
        val log =
            logService.createLogWithManyChunks(
                filename = "performance-test.log",
                userId = UUID.randomUUID(),
                chunkCount = chunkCount,
            )
        return log.id!!
    }
}
