package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.domain.entity.LogChunkEntity
import me.manulorenzo.loglens.api.domain.entity.LogEntity
import me.manulorenzo.loglens.api.domain.repository.LogChunkRepository
import me.manulorenzo.loglens.api.domain.repository.LogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class LogService(
    private val logChunkRepository: LogChunkRepository,
    private val logRepository: LogRepository,
) {
    /**
     * Create a new Log with its chunks in a single transaction.
     */
    @Transactional
    fun createLog(
        filename: String,
        userId: UUID,
        chunks: List<String>,
    ): LogEntity {
        val log = LogEntity(fileName = filename, userId = userId, uploadedAt = Instant.now())
        val savedLog = logRepository.save(log)
        val logEntityList =
            chunks.map {
                LogEntity(
                    fileName = filename,
                    userId = userId,
                    uploadedAt = Instant.now(),
                )
            }
        logRepository.saveAll(logEntityList)
        return savedLog
    }

    /**
     * Intentionally naive method to demonstrate N+1 problem.
     */
    @Transactional(readOnly = true)
    fun getLogWithChunksNaive(logId: UUID): LogEntity {
        val log =
            logRepository
                .findById(logId)
                .orElseThrow { RuntimeException("Log not found") }

        // 2. Access the lazy-loaded collection
        // This triggers a separate query for the chunks (N+1 problem if called in a loop)
        log.chunks.size

        return log
    }

    /**
     * Optimized method using fetch join or EntityGraph.
     */
    @Transactional(readOnly = true)
    fun getLogWithChunksOptimized(logId: UUID): LogEntity {
        // Uses the custom query with "JOIN FETCH" to get everything in 1 query
        return logRepository.findLogWithChunks(logId)
            ?: throw RuntimeException("Log not found")
    }

    /**
     * Demonstrates transaction rollback.
     */
    @Transactional
    fun createLogAndFail(
        filename: String,
        userId: UUID,
    ) {
        val log = LogEntity(fileName = filename, userId = userId, uploadedAt = Instant.now())
        logRepository.save(log)

        // This RuntimeException will cause the transaction to rollback.
        // The log saved above will NOT be persisted to the database.
        throw RuntimeException("Simulated failure")
    }

    /**
     * Update filename (used for optimistic locking test).
     */
    @Transactional
    fun updateLogFilename(
        logId: UUID,
        newFilename: String,
    ) {
        val log =
            logRepository
                .findById(logId)
                .orElseThrow { RuntimeException("Log not found") }

        // "Dirty Checking":
        // We modify the entity, and because we are in a transaction,
        // Hibernate detects the change and automatically issues an UPDATE statement at commit.
        // No explicit logRepository.save(log) is needed.
        log.fileName = newFilename
    }

    /**
     * Batch insert demo (performance test).
     */
    @Transactional
    fun createLogWithManyChunks(
        filename: String,
        userId: UUID,
        chunkCount: Int,
    ): LogEntity {
        val log =
            logRepository.save(
                LogEntity(
                    fileName = filename,
                    userId = userId,
                    uploadedAt = Instant.now(),
                ),
            )
        val chunks =
            (1..chunkCount).map {
                LogChunkEntity(
                    log = log,
                    content = "Chunk $it",
                )
            }
        logChunkRepository.saveAll(chunks)
        return log
    }
}
