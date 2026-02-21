package me.manulorenzo.worker

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthIT(
    @LocalServerPort val port: Int,
) {
    private val client = RestTemplate()

    @Test
    fun `health endpoint returns 200`() {
        val response = client.getForEntity("http://localhost:$port/health", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
    }
}
