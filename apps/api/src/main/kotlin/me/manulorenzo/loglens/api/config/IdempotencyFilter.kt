package me.manulorenzo.loglens.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.manulorenzo.loglens.api.service.IdempotencyKeyService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class IdempotencyFilter(
    private val idempotencyKeyService: IdempotencyKeyService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val idempotencyKey = request.getHeader("Idempotency-Key")

        if (idempotencyKey == null) {
            filterChain.doFilter(request, response)
            return
        }

        val cachedResponse = idempotencyKeyService.getResponse(idempotencyKey)
        if (cachedResponse != null) {
            response.outputStream.write(cachedResponse)
            return
        }

        val responseWrapper = ContentCachingResponseWrapper(response)
        filterChain.doFilter(request, responseWrapper)

        if (responseWrapper.status in 200..299) {
            idempotencyKeyService.saveResponse(idempotencyKey, responseWrapper.contentAsByteArray)
        }
        responseWrapper.copyBodyToResponse()
    }
}
