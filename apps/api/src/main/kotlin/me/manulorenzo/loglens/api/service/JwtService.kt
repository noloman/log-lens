package me.manulorenzo.loglens.api.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.manulorenzo.loglens.api.domain.entity.UserEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms:3600000}") private val expirationMs: Long,
) {
    init {
        require(secret.toByteArray(Charsets.UTF_8).size >= MIN_SECRET_LENGTH_BYTES) {
            "jwt.secret must be at least $MIN_SECRET_LENGTH_BYTES bytes long"
        }
        require(!secret.contains("change-me", ignoreCase = true)) {
            "jwt.secret must not use a placeholder value"
        }
    }

    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8)) }

    fun generateToken(user: UserEntity): String =
        Jwts
            .builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun validateToken(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

    fun getUserId(token: String): String = validateToken(token).subject

    companion object {
        private const val MIN_SECRET_LENGTH_BYTES = 32
    }
}
