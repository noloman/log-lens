package me.manulorenzo.loglens.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import me.manulorenzo.loglens.api.dto.CreateLogRequest
import me.manulorenzo.loglens.api.dto.LogResponse
import me.manulorenzo.loglens.api.service.LogEntryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/logs")
class LogController(
    private val logEntryService: LogEntryService,
) {
    @Operation(summary = "Create a log entry", description = "Ingests a new structured log entry")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Log entry created",
                content = [Content(schema = Schema(implementation = LogResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Bad request"),
        ],
    )
    @PostMapping
    fun createLog(
        @Valid @RequestBody request: CreateLogRequest,
        authentication: Authentication,
    ): ResponseEntity<LogResponse> {
        val userId = currentUserId(authentication)
        val response = logEntryService.createLog(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "List log entries", description = "Returns log entries, optionally filtered by service name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Log entries retrieved"),
        ],
    )
    @GetMapping
    fun getLogs(
        @RequestParam(required = false) serviceName: String?,
        authentication: Authentication,
    ): List<LogResponse> {
        val userId = currentUserId(authentication)
        return logEntryService.getLogs(userId, serviceName)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a log entry by ID", description = "Returns a single log entry by its ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Log entry retrieved",
                content = [Content(schema = Schema(implementation = LogResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "Log entry not found"),
        ],
    )
    fun getLog(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): LogResponse {
        val currentUserId = currentUserId(authentication)
        return logEntryService.getLogById(id, currentUserId)
    }

    private fun currentUserId(authentication: Authentication) = UUID.fromString(authentication.principal as String)
}
