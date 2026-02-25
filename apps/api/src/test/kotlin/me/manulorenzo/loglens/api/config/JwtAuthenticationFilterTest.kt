package me.manulorenzo.loglens.api.config

import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.manulorenzo.loglens.api.service.JwtService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {
    @InjectMocks
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should authenticate user with valid token`() {
        // Given
        val token = "valid-token"
        val userId = "user-id"
        val role = "USER"
        val claims = mock(Claims::class.java)
        
        `when`(request.getHeader("Authorization")).thenReturn("Bearer $token")
        `when`(jwtService.validateToken(token)).thenReturn(claims)
        `when`(claims.subject).thenReturn(userId)
        `when`(claims["role"]).thenReturn(role)

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Then
        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(userId, authentication.principal)
        assertEquals(1, authentication.authorities.size)
        assertEquals("ROLE_$role", authentication.authorities.first().authority)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should not authenticate user with invalid token`() {
        // Given
        val token = "invalid-token"
        `when`(request.getHeader("Authorization")).thenReturn("Bearer $token")
        `when`(jwtService.validateToken(token)).thenThrow(RuntimeException("Invalid token"))

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should not authenticate user with no token`() {
        // Given
        `when`(request.getHeader("Authorization")).thenReturn(null)

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should not authenticate user with malformed token`() {
        // Given
        val token = "malformed-token"
        `when`(request.getHeader("Authorization")).thenReturn(token)

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }
}
