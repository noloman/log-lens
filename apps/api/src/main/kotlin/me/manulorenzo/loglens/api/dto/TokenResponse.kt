package me.manulorenzo.loglens.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class TokenResponse(
    @Schema(description = "Access token JWT")
    val accessToken: String,
    @Schema(description = "Refresh token UUID")
    val refreshToken: String,
)
