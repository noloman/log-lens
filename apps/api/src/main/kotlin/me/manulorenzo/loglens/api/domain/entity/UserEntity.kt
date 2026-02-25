package me.manulorenzo.loglens.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(unique = true)
    val email: String,
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    val role: Role = Role.USER,
    val createdAt: Instant = Instant.now(),
) : UserDetails {
    override fun getUsername() = email

    override fun getPassword() = passwordHash

    override fun getAuthorities() = listOf(SimpleGrantedAuthority(role.name))

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = true
}
