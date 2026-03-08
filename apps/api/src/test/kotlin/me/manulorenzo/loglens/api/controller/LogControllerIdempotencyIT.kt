package me.manulorenzo.loglens.api.controller

import me.manulorenzo.loglens.api.domain.repository.LogEntryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LogControllerIdempotencyIT {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var logEntryRepository: LogEntryRepository

    private val client = RestTemplate()

    private lateinit var accessToken: String

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    @BeforeEach
    fun setUp() {
        logEntryRepository.deleteAll()

        val email = "logtest-${UUID.randomUUID()}@test.com"
        val authBody = """{"email":"$email","password":"password"}"""
        val authHeaders = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        client.postForEntity(
            "http://localhost:$port/v1/auth/register",
            HttpEntity(authBody, authHeaders),
            String::class.java,
        )

        val loginResponse = client.postForEntity(
            "http://localhost:$port/v1/auth/login",
            HttpEntity(authBody, authHeaders),
            Map::class.java,
        )

        @Suppress("UNCHECKED_CAST")
        val body = loginResponse.body as Map<String, String>
        accessToken = body["accessToken"]!!
    }

    @Test
    fun `POST logs with same idempotency key should create only one log entry`() {
        val idempotencyKey = UUID.randomUUID().toString()
        val logBody = """{"serviceName":"api","level":"INFO","message":"idempotent test"}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
            set("Idempotency-Key", idempotencyKey)
        }
        val request = HttpEntity(logBody, headers)

        // First request — creates the log entry
        val firstResponse = client.exchange(
            "http://localhost:$port/v1/logs",
            HttpMethod.POST,
            request,
            String::class.java,
        )
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)
        assertEquals(1, logEntryRepository.count())

        // Second request with the same idempotency key — returns cached response
        val secondResponse = client.exchange(
            "http://localhost:$port/v1/logs",
            HttpMethod.POST,
            request,
            String::class.java,
        )
        assertEquals(HttpStatus.OK, secondResponse.statusCode)
        assertEquals(firstResponse.body, secondResponse.body)

        // Verify no duplicate was created
        assertEquals(1, logEntryRepository.count())
    }

    @Test
    fun `POST logs with different idempotency keys should create separate log entries`() {
        val logBody = """{"serviceName":"api","level":"WARN","message":"different keys test"}"""

        val firstHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
            set("Idempotency-Key", UUID.randomUUID().toString())
        }
        val secondHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
            set("Idempotency-Key", UUID.randomUUID().toString())
        }

        val firstResponse = client.exchange(
            "http://localhost:$port/v1/logs",
            HttpMethod.POST,
            HttpEntity(logBody, firstHeaders),
            String::class.java,
        )
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)

        val secondResponse = client.exchange(
            "http://localhost:$port/v1/logs",
            HttpMethod.POST,
            HttpEntity(logBody, secondHeaders),
            String::class.java,
        )
        assertEquals(HttpStatus.CREATED, secondResponse.statusCode)

        // Two different keys = two log entries
        assertEquals(2, logEntryRepository.count())
    }

    @Test
    fun `POST logs without idempotency key should always create a new log entry`() {
        val logBody = """{"serviceName":"api","level":"ERROR","message":"no key test"}"""
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
        }
        val request = HttpEntity(logBody, headers)

        client.exchange("http://localhost:$port/v1/logs", HttpMethod.POST, request, String::class.java)
        client.exchange("http://localhost:$port/v1/logs", HttpMethod.POST, request, String::class.java)

        // No idempotency key = two separate entries
        assertEquals(2, logEntryRepository.count())
    }
}
