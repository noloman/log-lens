package me.manulorenzo.loglens.api.service

import io.jsonwebtoken.MalformedJwtException
import me.manulorenzo.loglens.api.domain.entity.Role
import me.manulorenzo.loglens.api.domain.entity.UserEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class JwtServiceTest {
    private lateinit var jwtService: JwtService
    private lateinit var user: UserEntity

    @BeforeEach
    fun setUp() {
        jwtService = JwtService("test-secret-key-that-is-long-enough-for-the-algorithm", 3600000)
        user =
            UserEntity(
                id = UUID.randomUUID(),
                email = "test@example.com",
                passwordHash = "password",
                role = Role.USER,
            )
    }

    @Test
    fun `should generate a valid token`() {
        // When
        val token = jwtService.generateToken(user)

        // Then
        assertNotNull(token)
    }

    @Test
    fun `should validate a token and return claims`() {
        // Given
        val token = jwtService.generateToken(user)

        // When
        val claims = jwtService.validateToken(token)

        // Then
        assertEquals(user.id.toString(), claims.subject)
        assertEquals(user.email, claims["email"])
        assertEquals(user.role.name, claims["role"])
    }

    @Test
    fun `should get user ID from token`() {
        // Given
        val token = jwtService.generateToken(user)

        // When
        val userId = jwtService.getUserId(token)

        // Then
        assertEquals(user.id.toString(), userId)
    }

    @Test
    fun `should throw exception for invalid token`() {
        // Given
        val invalidToken = "invalid-token"

        // When/Then
        assertThrows(MalformedJwtException::class.java) {
            jwtService.validateToken(invalidToken)
        }
    }

    @Test
    fun `should throw exception for expired token`() {
        // Given
        val expiredJwtService = JwtService("test-secret-key-that-is-long-enough-for-the-algorithm", 0)
        val token = expiredJwtService.generateToken(user)

        // When/Then
        assertThrows(io.jsonwebtoken.ExpiredJwtException::class.java) {
            jwtService.validateToken(token)
        }
    }
}
