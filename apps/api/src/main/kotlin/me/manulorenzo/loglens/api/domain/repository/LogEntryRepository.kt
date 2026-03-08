package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.LogEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LogEntryRepository : JpaRepository<LogEntryEntity, UUID> {
    fun findByServiceName(serviceName: String): List<LogEntryEntity>
}
