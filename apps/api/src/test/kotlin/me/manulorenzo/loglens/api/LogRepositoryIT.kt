package me.manulorenzo.loglens.api

import me.manulorenzo.loglens.api.domain.entity.LogEntity
import me.manulorenzo.loglens.api.domain.repository.LogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LogRepositoryIT {
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
        }
    }

    @Autowired
    lateinit var logRepository: LogRepository

    @Test
    fun `should save and load log with chunks`() {
        val log =
            LogEntity(
                fileName = "test.log",
                userId = UUID.randomUUID(),
                uploadedAt = Instant.now(),
            )

        val savedLog = logRepository.save(log)
        assertThat(savedLog.id).isNotNull()
    }
}
