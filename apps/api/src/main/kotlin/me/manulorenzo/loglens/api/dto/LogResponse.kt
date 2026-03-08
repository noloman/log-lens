package me.manulorenzo.loglens.api.dto

import java.time.Instant
import java.util.UUID

data class LogResponse(
    val id: UUID,
    val serviceName: String,
    val level: String,
    val message: String,
    val timestamp: Instant
)
