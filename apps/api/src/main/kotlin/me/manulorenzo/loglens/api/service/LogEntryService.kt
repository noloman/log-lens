package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.domain.entity.LogEntryEntity
import me.manulorenzo.loglens.api.domain.repository.LogEntryRepository
import me.manulorenzo.loglens.api.dto.CreateLogRequest
import me.manulorenzo.loglens.api.dto.LogResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogEntryService(
    private val logEntryRepository: LogEntryRepository,
) {
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

    private fun toResponse(entity: LogEntryEntity) =
        LogResponse(
            id = entity.id!!,
            serviceName = entity.serviceName,
            level = entity.level,
            message = entity.message,
            timestamp = entity.timestamp,
        )
}
