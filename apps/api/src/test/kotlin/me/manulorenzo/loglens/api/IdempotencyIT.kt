package me.manulorenzo.loglens.api

import me.manulorenzo.loglens.api.domain.repository.UserRepository
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IdempotencyIT {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var userRepository: UserRepository

    private val client = RestTemplate()

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
        userRepository.deleteAll()
    }

    @Test
    fun `given the same idempotency key, the second request should return the cached response and not create a new user`() {
        val idempotencyKey = UUID.randomUUID().toString()
        val headers =
            HttpHeaders().apply {
                set("Idempotency-Key", idempotencyKey)
                set("Content-Type", "application/json")
            }
        val email = "idempotent@test.com"
        val registerRequest = """{"email":"$email","password":"password"}"""
        val requestEntity = HttpEntity(registerRequest, headers)

        // First request
        val firstResponse =
            client.exchange(
                "http://localhost:$port/v1/auth/register",
                HttpMethod.POST,
                requestEntity,
                String::class.java,
            )
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)
        assertEquals(1, userRepository.count())

        // Second request with the same idempotency key
        val secondResponse =
            client.exchange(
                "http://localhost:$port/v1/auth/register",
                HttpMethod.POST,
                requestEntity,
                String::class.java,
            )
        assertEquals(HttpStatus.OK, secondResponse.statusCode)
        assertEquals(firstResponse.body, secondResponse.body)

        // Verify that no new user was created
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `given different idempotency keys, two requests should create two different users`() {
        val idempotencyKey1 = UUID.randomUUID().toString()
        val headers1 =
            HttpHeaders().apply {
                set("Idempotency-Key", idempotencyKey1)
                set("Content-Type", "application/json")
            }
        val email1 = "idempotent1@test.com"
        val registerRequest1 = """{"email":"$email1","password":"password"}"""
        val requestEntity1 = HttpEntity(registerRequest1, headers1)

        val firstResponse =
            client.exchange(
                "http://localhost:$port/v1/auth/register",
                HttpMethod.POST,
                requestEntity1,
                String::class.java,
            )
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)
        assertEquals(1, userRepository.count())

        val idempotencyKey2 = UUID.randomUUID().toString()
        val headers2 =
            HttpHeaders().apply {
                set("Idempotency-Key", idempotencyKey2)
                set("Content-Type", "application/json")
            }
        val email2 = "idempotent2@test.com"
        val registerRequest2 = """{"email":"$email2","password":"password"}"""
        val requestEntity2 = HttpEntity(registerRequest2, headers2)

        val secondResponse =
            client.exchange(
                "http://localhost:$port/v1/auth/register",
                HttpMethod.POST,
                requestEntity2,
                String::class.java,
            )
        assertEquals(HttpStatus.CREATED, secondResponse.statusCode)
        assertEquals(2, userRepository.count())
    }
}
