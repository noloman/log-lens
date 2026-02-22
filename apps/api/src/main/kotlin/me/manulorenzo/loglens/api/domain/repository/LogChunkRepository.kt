package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.LogChunkEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LogChunkRepository : JpaRepository<LogChunkEntity, UUID>
