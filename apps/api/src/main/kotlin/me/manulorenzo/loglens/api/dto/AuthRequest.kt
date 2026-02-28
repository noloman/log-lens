package me.manulorenzo.loglens.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AuthRequest(
    @Schema(example = "user@example.com", description = "User email address")
    val email: String,
    @Schema(example = "password123", description = "User password")
    val password: String,
)
