package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.domain.entity.RefreshTokenEntity
import me.manulorenzo.loglens.api.domain.entity.UserEntity
import me.manulorenzo.loglens.api.domain.repository.RefreshTokenRepository
import me.manulorenzo.loglens.api.domain.repository.UserRepository
import me.manulorenzo.loglens.api.error.exception.UnauthorizedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @InjectMocks
    private lateinit var authService: AuthService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtService: JwtService

    private lateinit var userEntity: UserEntity

    @BeforeEach
    fun setUp() {
        userEntity = UserEntity(email = "testuser", passwordHash = "testpassword")
    }

    @Test
    fun `should return a token when authenticating with valid credentials`() {
        // Given
        val username = "testuser"
        val password = "testpassword"
        `when`(userRepository.findByEmail(username)).thenReturn(userEntity)
        `when`(passwordEncoder.matches(password, userEntity.passwordHash)).thenReturn(true)
        `when`(jwtService.generateToken(userEntity)).thenReturn("test-token")

        // When
        val token = authService.login(username, password)

        // Then
        assertNotNull(token)
        assertEquals("test-token", token.accessToken)
    }

    @Test
    fun `should throw UnauthorizedException when authenticating with invalid password`() {
        // Given
        val username = "testuser"
        val password = "wrongpassword"
        `when`(userRepository.findByEmail(username)).thenReturn(userEntity)
        `when`(passwordEncoder.matches(password, userEntity.passwordHash)).thenReturn(false)

        // When/Then
        assertThrows(UnauthorizedException::class.java) {
            authService.login(username, password)
        }
    }

    @Test
    fun `should throw UnauthorizedException when authenticating with non-existent user`() {
        // Given
        val username = "nonexistent"
        val password = "password"
        `when`(userRepository.findByEmail(username)).thenReturn(null)

        // When/Then
        assertThrows(UnauthorizedException::class.java) {
            authService.login(username, password)
        }
    }

    @Test
    fun `should register a new user`() {
        // Given
        val username = "newuser"
        val password = "password"
        val encodedPassword = "encodedPassword"
        `when`(passwordEncoder.encode(password)).thenReturn(encodedPassword)
        `when`(userRepository.save(any(UserEntity::class.java))).thenAnswer { it.arguments[0] }

        // When
        val user = authService.register(username, password)

        // Then
        assertNotNull(user)
        assertEquals(username, user.email)
        assertEquals(encodedPassword, user.passwordHash)
    }

    @Test
    fun `should refresh token`() {
        // Given
        val refreshToken = UUID.randomUUID()
        val refreshTokenEntity =
            RefreshTokenEntity(
                token = refreshToken,
                user = userEntity,
                expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            )
        `when`(refreshTokenRepository.findByToken(refreshToken)).thenReturn(refreshTokenEntity)
        `when`(jwtService.generateToken(userEntity)).thenReturn("new-access-token")

        // When
        val tokenResponse = authService.refresh(refreshToken)

        // Then
        assertNotNull(tokenResponse)
        assertEquals("new-access-token", tokenResponse.accessToken)
        assertEquals(refreshToken.toString(), tokenResponse.refreshToken)
    }

    @Test
    fun `should throw UnauthorizedException when refreshing with invalid token`() {
        // Given
        val refreshToken = UUID.randomUUID()
        `when`(refreshTokenRepository.findByToken(refreshToken)).thenReturn(null)

        // When/Then
        assertThrows(UnauthorizedException::class.java) {
            authService.refresh(refreshToken)
        }
    }

    @Test
    fun `should throw UnauthorizedException when refreshing with expired token`() {
        // Given
        val refreshToken = UUID.randomUUID()
        val refreshTokenEntity =
            RefreshTokenEntity(
                token = refreshToken,
                user = userEntity,
                expiresAt = Instant.now().minus(1, ChronoUnit.DAYS),
            )
        `when`(refreshTokenRepository.findByToken(refreshToken)).thenReturn(refreshTokenEntity)

        // When/Then
        assertThrows(UnauthorizedException::class.java) {
            authService.refresh(refreshToken)
        }
    }

    @Test
    fun `should logout`() {
        // Given
        val refreshToken = UUID.randomUUID()
        val refreshTokenEntity =
            RefreshTokenEntity(
                token = refreshToken,
                user = userEntity,
                expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            )
        `when`(refreshTokenRepository.findByToken(refreshToken)).thenReturn(refreshTokenEntity)

        // When
        authService.logout(refreshToken)

        // Then
        verify(refreshTokenRepository).delete(refreshTokenEntity)
    }
}
