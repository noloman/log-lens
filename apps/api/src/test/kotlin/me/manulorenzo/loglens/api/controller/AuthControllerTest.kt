package me.manulorenzo.loglens.api.controller

import me.manulorenzo.loglens.api.dto.TokenResponse
import me.manulorenzo.loglens.api.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(
    AuthController::class,
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
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var authService: AuthService

    @Test
    fun `should return 200 OK for login`() {
        `when`(authService.login(anyString(), anyString())).thenReturn(
            TokenResponse(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
        )
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "user", "password": "password"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("accessToken"))
            .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
    }

    @Test
    fun `should return 201 Created for register`() {
        mockMvc
            .perform(
                post("/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "user", "password": "password"}"""),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `should return 200 OK for refresh`() {
        val uuid = UUID.randomUUID()
        `when`(authService.refresh(uuid)).thenReturn(
            TokenResponse(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
        )
        mockMvc
            .perform(
                post("/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken": "$uuid"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("accessToken"))
            .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
    }
}
