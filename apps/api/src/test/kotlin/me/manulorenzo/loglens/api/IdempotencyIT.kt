package me.manulorenzo.loglens.api

import me.manulorenzo.loglens.api.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `given the same idempotency key, the second request should return the cached response and not create a new user`() {
        val idempotencyKey = UUID.randomUUID().toString()
        val headers = HttpHeaders().apply {
            set("Idempotency-Key", idempotencyKey)
            set("Content-Type", "application/json")
        }
        val email = "idempotent@test.com"
        val registerRequest = """{"email":"$email","password":"password"}"""
        val requestEntity = HttpEntity(registerRequest, headers)

        // First request
        val firstResponse = client.exchange("http://localhost:$port/v1/auth/register", HttpMethod.POST, requestEntity, String::class.java)
        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)
        assertEquals(1, userRepository.count())

        // Second request with the same idempotency key
        val secondResponse = client.exchange("http://localhost:$port/v1/auth/register", HttpMethod.POST, requestEntity, String::class.java)
        assertEquals(HttpStatus.CREATED, secondResponse.statusCode)
        assertEquals(firstResponse.body, secondResponse.body)

        // Verify that no new user was created
        assertEquals(1, userRepository.count())
    }
}
