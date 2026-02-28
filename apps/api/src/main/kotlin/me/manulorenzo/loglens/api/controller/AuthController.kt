package me.manulorenzo.loglens.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import me.manulorenzo.loglens.api.dto.AuthRequest
import me.manulorenzo.loglens.api.dto.RefreshRequest
import me.manulorenzo.loglens.api.dto.TokenResponse
import me.manulorenzo.loglens.api.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(summary = "Login", description = "Authenticate a user and return access and refresh tokens")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody authRequest: AuthRequest,
    ) = authService.login(authRequest.email, authRequest.password)

    @Operation(summary = "Register", description = "Register a new user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "User registered successfully"),
            ApiResponse(responseCode = "400", description = "Bad request")
        ]
    )
    @PostMapping("/register")
    fun register(
        @RequestBody authRequest: AuthRequest,
    ): ResponseEntity<Unit> {
        authService.register(authRequest.email, authRequest.password)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(summary = "Refresh token", description = "Refresh an expired access token using a refresh token")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token refreshed successfully",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest,
    ) = authService.refresh(refreshRequest.refreshToken)
}
