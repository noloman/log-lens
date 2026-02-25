package me.manulorenzo.loglens.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.manulorenzo.loglens.api.service.JwtService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")

        if (token != null) {
            try {
                val claims = jwtService.validateToken(token)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims["role"]}"))
                val auth = UsernamePasswordAuthenticationToken(claims.subject, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                // Invalid token — let the request continue unauthenticated
            }
        }

        filterChain.doFilter(request, response)
    }
}
