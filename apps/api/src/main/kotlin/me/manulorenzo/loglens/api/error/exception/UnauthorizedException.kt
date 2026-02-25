package me.manulorenzo.loglens.api.error.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
