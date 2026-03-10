package me.manulorenzo.loglens.api.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = request.remoteAddr
        val bucket = buckets.computeIfAbsent(key) { createBucket() }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.setHeader("Retry-After", "60")
            response.writer.write("""{"error":"Rate limit exceeded"}""")
        }
    }

    private fun createBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(100)
                    .refillGreedy(100, Duration.ofMinutes(1))
                    .build(),
            ).build()
}
