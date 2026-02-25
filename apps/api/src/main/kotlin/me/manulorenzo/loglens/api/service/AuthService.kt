package me.manulorenzo.loglens.api.service

import jakarta.transaction.Transactional
import me.manulorenzo.loglens.api.domain.entity.RefreshTokenEntity
import me.manulorenzo.loglens.api.domain.entity.UserEntity
import me.manulorenzo.loglens.api.domain.repository.RefreshTokenRepository
import me.manulorenzo.loglens.api.domain.repository.UserRepository
import me.manulorenzo.loglens.api.dto.TokenResponse
import me.manulorenzo.loglens.api.error.exception.UnauthorizedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(
        username: String,
        password: String,
    ): UserEntity {
        val hash = passwordEncoder.encode(password)
        val user = UserEntity(email = username, passwordHash = hash)
        return userRepository.save(user)
    }

    @Transactional
    fun login(
        username: String,
        password: String,
    ): TokenResponse {
        val user = userRepository.findByEmail(username) ?: throw UnauthorizedException()
        if (!passwordEncoder.matches(password, user.passwordHash)) throw UnauthorizedException()

        val accessToken = jwtService.generateToken(user)
        val refreshToken = UUID.randomUUID()

        refreshTokenRepository.save(
            RefreshTokenEntity(
                token = refreshToken,
                user = user,
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
            ),
        )

        return TokenResponse(accessToken, refreshToken.toString())
    }

    @Transactional
    fun refresh(refreshToken: UUID): TokenResponse {
        val refreshTokenEntity =
            refreshTokenRepository.findByToken(refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")
        if (refreshTokenEntity.expiresAt < Instant.now()) {
            throw UnauthorizedException("Refresh token expired")
        }

        val newAccessToken = jwtService.generateToken(refreshTokenEntity.user)

        return TokenResponse(newAccessToken, refreshToken.toString())
    }

    @Transactional
    fun logout(refreshToken: UUID) {
        val refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken) ?: return
        refreshTokenRepository.delete(refreshTokenEntity)
    }
}
