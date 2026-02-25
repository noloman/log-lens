package me.manulorenzo.loglens.api.dto

import java.util.UUID

data class RefreshRequest(
    val refreshToken: UUID,
)
