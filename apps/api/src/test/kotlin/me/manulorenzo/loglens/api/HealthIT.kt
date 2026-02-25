package me.manulorenzo.loglens.api

import me.manulorenzo.loglens.api.controller.HealthController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
    controllers = [HealthController::class],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [me.manulorenzo.loglens.api.config.SecurityConfig::class]),
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [me.manulorenzo.loglens.api.config.JwtAuthenticationFilter::class]),
    ],
)
class HealthIT(
    @Autowired val mockMvc: MockMvc,
) {
    @Test
    fun `health endpoint returns 200`() {
        mockMvc
            .perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
    }
}
