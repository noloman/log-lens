package me.manulorenzo.loglens.api.dto

import jakarta.validation.constraints.NotBlank

data class CreateLogRequest(
    @field:NotBlank(message = "Service name cannot be blank")
    val serviceName: String,
    @field:NotBlank(message = "Log level cannot be blank")
    val level: String,
    @field:NotBlank(message = "Log message cannot be blank")
    val message: String
)
