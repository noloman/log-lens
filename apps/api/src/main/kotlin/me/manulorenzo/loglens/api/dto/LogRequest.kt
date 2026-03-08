package me.manulorenzo.loglens.api.dto

import jakarta.validation.constraints.NotBlank

data class LogRequest(
    @field:NotBlank(message = "Log message cannot be blank")
    val message: String
)
