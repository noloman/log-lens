package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.LogEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface LogEntryRepository : JpaRepository<LogEntryEntity, UUID> {
    fun findByServiceName(serviceName: String): List<LogEntryEntity>
    fun findByUserId(userId: UUID): List<LogEntryEntity>
    fun findByUserIdAndServiceName(userId: UUID, serviceName: String): List<LogEntryEntity>
    fun findByIdAndUserId(id: UUID, userId: UUID): Optional<LogEntryEntity>
}
