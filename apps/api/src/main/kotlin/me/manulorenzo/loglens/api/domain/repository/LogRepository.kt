package me.manulorenzo.loglens.api.domain.repository

import me.manulorenzo.loglens.api.domain.entity.LogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LogRepository : JpaRepository<LogEntity, UUID> {
    @Query("SELECT l FROM LogEntity l JOIN FETCH l.chunks WHERE l.id = :id")
    fun findLogWithChunks(
        @Param("id") id: UUID,
    ): LogEntity?
}
