package me.manulorenzo.loglens.api.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
