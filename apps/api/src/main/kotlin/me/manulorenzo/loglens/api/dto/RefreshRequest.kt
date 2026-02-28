package me.manulorenzo.loglens.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class RefreshRequest(
    @Schema(description = "Refresh token UUID")
    val refreshToken: UUID,
)
