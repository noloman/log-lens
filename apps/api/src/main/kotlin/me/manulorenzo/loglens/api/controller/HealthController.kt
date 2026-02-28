package me.manulorenzo.loglens.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import me.manulorenzo.loglens.api.dto.HealthResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @Operation(summary = "Health check", description = "Returns the health status of the service")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Service is healthy",
                content = [Content(schema = Schema(implementation = HealthResponse::class))]
            )
        ]
    )
    @GetMapping("/health")
    fun health() = HealthResponse(status = "ok", service = "loglens-api")
}
