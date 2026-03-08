package me.manulorenzo.loglens.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.manulorenzo.loglens.api.domain.repository.LogEntryRepository
import me.manulorenzo.loglens.api.dto.LogResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LogControllerIT {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var logEntryRepository: LogEntryRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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

    private fun baseUrl() = "http://localhost:$port"

    private fun authedHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setBearerAuth(accessToken)
    }

    private fun postLog(body: String, headers: HttpHeaders = authedHeaders()) =
        client.exchange("${baseUrl()}/v1/logs", HttpMethod.POST, HttpEntity(body, headers), String::class.java)

    private fun getLogs(serviceName: String? = null, headers: HttpHeaders = authedHeaders()): org.springframework.http.ResponseEntity<String> {
        val url = if (serviceName != null) "${baseUrl()}/v1/logs?serviceName=$serviceName" else "${baseUrl()}/v1/logs"
        return client.exchange(url, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
    }

    @BeforeEach
    fun setUp() {
        logEntryRepository.deleteAll()

        val email = "logit-${UUID.randomUUID()}@test.com"
        val authBody = """{"email":"$email","password":"password"}"""
        val authHeaders = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        client.postForEntity("${baseUrl()}/v1/auth/register", HttpEntity(authBody, authHeaders), String::class.java)
        val loginResponse = client.postForEntity("${baseUrl()}/v1/auth/login", HttpEntity(authBody, authHeaders), Map::class.java)

        @Suppress("UNCHECKED_CAST")
        accessToken = (loginResponse.body as Map<String, String>)["accessToken"]!!
    }

    @Nested
    inner class PostLogs {
        @Test
        fun `should create a log entry and return 201 with response body`() {
            val body = """{"serviceName":"api","level":"INFO","message":"user logged in"}"""

            val response = postLog(body)

            assertEquals(HttpStatus.CREATED, response.statusCode)

            val logResponse = objectMapper.readValue(response.body, LogResponse::class.java)
            assertNotNull(logResponse.id)
            assertEquals("api", logResponse.serviceName)
            assertEquals("INFO", logResponse.level)
            assertEquals("user logged in", logResponse.message)
            assertNotNull(logResponse.timestamp)
        }

        @Test
        fun `should persist the log entry in the database`() {
            val body = """{"serviceName":"worker","level":"WARN","message":"disk space low"}"""

            postLog(body)

            val entries = logEntryRepository.findByServiceName("worker")
            assertEquals(1, entries.size)
            assertEquals("WARN", entries[0].level)
            assertEquals("disk space low", entries[0].message)
        }

        @Test
        fun `should create multiple independent log entries`() {
            postLog("""{"serviceName":"api","level":"INFO","message":"first"}""")
            postLog("""{"serviceName":"api","level":"ERROR","message":"second"}""")
            postLog("""{"serviceName":"worker","level":"DEBUG","message":"third"}""")

            assertEquals(3, logEntryRepository.count())
            assertEquals(2, logEntryRepository.findByServiceName("api").size)
            assertEquals(1, logEntryRepository.findByServiceName("worker").size)
        }
    }

    @Nested
    inner class GetLogs {
        @Test
        fun `should return empty list when no logs exist`() {
            val response = getLogs()

            assertEquals(HttpStatus.OK, response.statusCode)
            val logs = objectMapper.readValue(response.body, Array<LogResponse>::class.java)
            assertEquals(0, logs.size)
        }

        @Test
        fun `should return all log entries`() {
            postLog("""{"serviceName":"api","level":"INFO","message":"one"}""")
            postLog("""{"serviceName":"worker","level":"WARN","message":"two"}""")

            val response = getLogs()

            assertEquals(HttpStatus.OK, response.statusCode)
            val logs = objectMapper.readValue(response.body, Array<LogResponse>::class.java)
            assertEquals(2, logs.size)
        }

        @Test
        fun `should filter by serviceName`() {
            postLog("""{"serviceName":"api","level":"INFO","message":"api log"}""")
            postLog("""{"serviceName":"worker","level":"ERROR","message":"worker log"}""")
            postLog("""{"serviceName":"api","level":"DEBUG","message":"another api log"}""")

            val response = getLogs(serviceName = "api")

            assertEquals(HttpStatus.OK, response.statusCode)
            val logs = objectMapper.readValue(response.body, Array<LogResponse>::class.java)
            assertEquals(2, logs.size)
            assertTrue(logs.all { it.serviceName == "api" })
        }

        @Test
        fun `should return empty list for unknown serviceName`() {
            postLog("""{"serviceName":"api","level":"INFO","message":"exists"}""")

            val response = getLogs(serviceName = "nonexistent")

            assertEquals(HttpStatus.OK, response.statusCode)
            val logs = objectMapper.readValue(response.body, Array<LogResponse>::class.java)
            assertEquals(0, logs.size)
        }
    }

    @Nested
    inner class Security {
        @Test
        fun `POST logs without token should return 403`() {
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val body = """{"serviceName":"api","level":"INFO","message":"no auth"}"""

            try {
                client.exchange("${baseUrl()}/v1/logs", HttpMethod.POST, HttpEntity(body, headers), String::class.java)
            } catch (e: HttpClientErrorException) {
                assertEquals(HttpStatus.FORBIDDEN, e.statusCode)
            }

            assertEquals(0, logEntryRepository.count())
        }

        @Test
        fun `GET logs without token should return 403`() {
            try {
                client.exchange("${baseUrl()}/v1/logs", HttpMethod.GET, HttpEntity<Void>(HttpHeaders()), String::class.java)
            } catch (e: HttpClientErrorException) {
                assertEquals(HttpStatus.FORBIDDEN, e.statusCode)
            }
        }

        @Test
        fun `POST logs with invalid token should return 403`() {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth("invalid.jwt.token")
            }
            val body = """{"serviceName":"api","level":"INFO","message":"bad token"}"""

            try {
                client.exchange("${baseUrl()}/v1/logs", HttpMethod.POST, HttpEntity(body, headers), String::class.java)
            } catch (e: HttpClientErrorException) {
                assertEquals(HttpStatus.FORBIDDEN, e.statusCode)
            }

            assertEquals(0, logEntryRepository.count())
        }
    }
}
