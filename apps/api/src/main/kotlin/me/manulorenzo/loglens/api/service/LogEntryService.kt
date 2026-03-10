package me.manulorenzo.loglens.api.service

import jakarta.persistence.EntityNotFoundException
import me.manulorenzo.loglens.api.domain.entity.LogEntryEntity
import me.manulorenzo.loglens.api.domain.repository.LogEntryRepository
import me.manulorenzo.loglens.api.dto.CreateLogRequest
import me.manulorenzo.loglens.api.dto.LogResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LogEntryService(
    private val logEntryRepository: LogEntryRepository,
) {
    @CacheEvict(value = ["logs"], allEntries = true)
    @Transactional
    fun createLog(request: CreateLogRequest): LogResponse {
        val entity =
            LogEntryEntity(
                serviceName = request.serviceName,
                level = request.level,
                message = request.message,
            )
        val saved = logEntryRepository.save(entity)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getLogs(serviceName: String?): List<LogResponse> {
        val entities =
            if (serviceName != null) {
                logEntryRepository.findByServiceName(serviceName)
            } else {
                logEntryRepository.findAll()
            }
        return entities.map(::toResponse)
    }

    @Cacheable(value = ["logs"], key = "#id")
    @Transactional(readOnly = true)
    fun getLogById(id: UUID): LogResponse {
        val entity =
            logEntryRepository.findById(id).orElseThrow {
                EntityNotFoundException("Log entry with ID $id not found")
            }
        return toResponse(entity)
    }

    private fun toResponse(entity: LogEntryEntity) =
        LogResponse(
            id = entity.id!!,
            serviceName = entity.serviceName,
            level = entity.level,
            message = entity.message,
            timestamp = entity.timestamp,
        )
}
