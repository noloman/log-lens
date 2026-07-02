package me.manulorenzo.loglens.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.manulorenzo.loglens.api.dto.CreateLogRequest
import me.manulorenzo.loglens.api.dto.LogResponse
import me.manulorenzo.loglens.api.service.LogEntryService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    LogController::class,
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [me.manulorenzo.loglens.api.config.SecurityConfig::class],
        ),
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [me.manulorenzo.loglens.api.config.JwtAuthenticationFilter::class],
        ),
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [me.manulorenzo.loglens.api.config.IdempotencyFilter::class],
        ),
    ],
)
@AutoConfigureMockMvc(addFilters = false)
class LogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var logEntryService: LogEntryService

    @Test
    fun `POST logs should return 201 with created log entry`() {
        val request = CreateLogRequest(serviceName = "api", level = "INFO", message = "test log")
        val response = LogResponse(
            id = UUID.randomUUID(),
            serviceName = "api",
            level = "INFO",
            message = "test log",
            timestamp = Instant.now(),
        )

        val userId = UUID.randomUUID()
        val auth = UsernamePasswordAuthenticationToken(userId.toString(), null)
        `when`(logEntryService.createLog(userId, request)).thenReturn(response)

        mockMvc.perform(
            post("/v1/logs")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.serviceName").value("api"))
            .andExpect(jsonPath("$.level").value("INFO"))
            .andExpect(jsonPath("$.message").value("test log"))
    }

    @Test
    fun `GET logs should return 200 with log entries`() {
        val response = LogResponse(
            id = UUID.randomUUID(),
            serviceName = "api",
            level = "INFO",
            message = "test log",
            timestamp = Instant.now(),
        )
        val userId = UUID.randomUUID()
        val auth = UsernamePasswordAuthenticationToken(userId.toString(), null)
        `when`(logEntryService.getLogs(userId, null)).thenReturn(listOf(response))

        mockMvc.perform(
            get("/v1/logs")
                .principal(auth)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].serviceName").value("api"))
    }

    @Test
    fun `GET logs with serviceName filter should pass filter to service`() {
        val userId = UUID.randomUUID()
        val auth = UsernamePasswordAuthenticationToken(userId.toString(), null)
        `when`(logEntryService.getLogs(userId, "worker")).thenReturn(emptyList())

        mockMvc.perform(
            get("/v1/logs")
                .principal(auth)
                .param("serviceName", "worker")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty())
    }

    @Test
    fun `POST logs with invalid data should fail`() {
        val userId = UUID.randomUUID()
        val auth = UsernamePasswordAuthenticationToken(userId.toString(), null)

        mockMvc.perform(
            post("/v1/logs")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"serviceName":"","level":"INFO","message":"test log"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }
}
