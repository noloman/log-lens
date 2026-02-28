package me.manulorenzo.loglens.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class HealthResponse(
    @Schema(example = "ok")
    val status: String,
    @Schema(example = "loglens-api")
    val service: String
)
