package me.manulorenzo.loglens.api.error

import me.manulorenzo.loglens.api.config.IdempotencyFilter
import me.manulorenzo.loglens.api.config.JwtAuthenticationFilter
import me.manulorenzo.loglens.api.config.SecurityConfig
import me.manulorenzo.loglens.api.error.exception.UnauthorizedException
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
    GlobalExceptionHandlerTestController::class,
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SecurityConfig::class],
        ),
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [JwtAuthenticationFilter::class],
        ),
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [IdempotencyFilter::class],
        ),
    ],
)
class GlobalExceptionHandlerTestIT(
    @param:Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `handleUnauthorized returns sanitized unauthorized problem detail`() {
        mockMvc
            .perform(get("/test/unauthorized"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value(not("raw auth failure detail")))
    }

    @Test
    fun `handleAllExceptions returns sanitized internal server error problem detail`() {
        mockMvc
            .perform(get("/test/unexpected-error"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("Internal Server Error"))
            .andExpect(jsonPath("$.detail").value(not("database password is xyz")))
    }
}

@RestController
class GlobalExceptionHandlerTestController {
    @GetMapping("/test/unauthorized")
    fun unauthorized() {
        throw UnauthorizedException("raw auth failure detail")
    }

    @GetMapping("/test/unexpected-error")
    fun unexpectedError() {
        throw RuntimeException("database password is xyz")
    }
}
