package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.IdempotencyKeyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IdempotencyKeyRepository : JpaRepository<IdempotencyKeyEntity, String>
