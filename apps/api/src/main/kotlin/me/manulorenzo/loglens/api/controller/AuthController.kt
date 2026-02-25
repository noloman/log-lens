package me.manulorenzo.loglens.api.controller

import me.manulorenzo.loglens.api.dto.AuthRequest
import me.manulorenzo.loglens.api.dto.RefreshRequest
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
    @PostMapping("/login")
    fun login(
        @RequestBody authRequest: AuthRequest,
    ) = authService.login(authRequest.email, authRequest.password)

    @PostMapping("/register")
    fun register(
        @RequestBody authRequest: AuthRequest,
    ): ResponseEntity<Unit> {
        authService.register(authRequest.email, authRequest.password)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest,
    ) = authService.refresh(refreshRequest.refreshToken)
}
