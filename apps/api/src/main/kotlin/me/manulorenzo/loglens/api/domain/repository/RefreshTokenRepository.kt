package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.RefreshTokenEntity
import me.manulorenzo.loglens.api.domain.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByUser(user: UserEntity): RefreshTokenEntity?

    fun findByToken(token: UUID): RefreshTokenEntity?
}
