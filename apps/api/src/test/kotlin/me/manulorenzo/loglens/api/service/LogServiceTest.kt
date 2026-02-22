package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.domain.entity.LogChunkEntity
import me.manulorenzo.loglens.api.domain.entity.LogEntity
import me.manulorenzo.loglens.api.domain.repository.LogChunkRepository
import me.manulorenzo.loglens.api.domain.repository.LogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LogServiceTest {
    @Mock
    lateinit var logRepository: LogRepository

    @Mock
    lateinit var logChunkRepository: LogChunkRepository

    @InjectMocks
    lateinit var logService: LogService

    @Test
    fun `createLog should associate chunks and save parent log`() {
        val filename = "test.log"
        val userId = UUID.randomUUID()
        val chunks = listOf("chunk1", "chunk2")
        val logCaptor = ArgumentCaptor.forClass(LogEntity::class.java)

        `when`(logRepository.save(logCaptor.capture())).thenAnswer { it.getArgument(0) }

        val result = logService.createLog(filename, userId, chunks)

        assertThat(result.fileName).isEqualTo(filename)
        assertThat(result.userId).isEqualTo(userId)
        verify(logRepository).save(any(LogEntity::class.java))

        val capturedLog = logCaptor.value
        assertThat(capturedLog.chunks).hasSize(2)
        assertThat(capturedLog.chunks.map { it.content }).containsExactlyInAnyOrder("chunk1", "chunk2")
    }

    @Test
    fun `getLogWithChunksNaive should return log when found`() {
        val logId = UUID.randomUUID()
        val logEntity = LogEntity(fileName = "test.log", userId = UUID.randomUUID(), uploadedAt = Instant.now())

        `when`(logRepository.findById(logId)).thenReturn(Optional.of(logEntity))

        val result = logService.getLogWithChunksNaive(logId)

        assertThat(result).isEqualTo(logEntity)
    }

    @Test
    fun `getLogWithChunksNaive should throw exception when not found`() {
        val logId = UUID.randomUUID()
        `when`(logRepository.findById(logId)).thenReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            logService.getLogWithChunksNaive(logId)
        }
    }

    @Test
    fun `getLogWithChunksOptimized should return log when found`() {
        val logId = UUID.randomUUID()
        val logEntity = LogEntity(fileName = "test.log", userId = UUID.randomUUID(), uploadedAt = Instant.now())
        `when`(logRepository.findLogWithChunks(logId)).thenReturn(logEntity)

        val result = logService.getLogWithChunksOptimized(logId)

        assertThat(result).isEqualTo(logEntity)
    }

    @Test
    fun `getLogWithChunksOptimized should throw exception when not found`() {
        val logId = UUID.randomUUID()
        `when`(logRepository.findLogWithChunks(logId)).thenReturn(null)

        assertThrows(RuntimeException::class.java) {
            logService.getLogWithChunksOptimized(logId)
        }
    }

    @Test
    fun `createLogAndFail should save log and throw exception`() {
        val filename = "test.log"
        val userId = UUID.randomUUID()

        assertThrows(RuntimeException::class.java) {
            logService.createLogAndFail(filename, userId)
        }

        verify(logRepository).save(any(LogEntity::class.java))
    }

    @Test
    fun `updateLogFilename should update filename when log found`() {
        val logId = UUID.randomUUID()
        val oldFilename = "old.log"
        val newFilename = "new.log"
        val logEntity = LogEntity(fileName = oldFilename, userId = UUID.randomUUID(), uploadedAt = Instant.now())

        `when`(logRepository.findById(logId)).thenReturn(Optional.of(logEntity))

        logService.updateLogFilename(logId, newFilename)

        assertThat(logEntity.fileName).isEqualTo(newFilename)
    }

    @Test
    fun `createLogWithManyChunks should save log and chunks`() {
        val filename = "test.log"
        val userId = UUID.randomUUID()
        val chunkCount = 5
        val logEntity = LogEntity(fileName = filename, userId = userId, uploadedAt = Instant.now())

        `when`(logRepository.save(any(LogEntity::class.java))).thenReturn(logEntity)
        `when`(logChunkRepository.saveAll(anyList())).thenReturn(emptyList<LogChunkEntity>())

        val result = logService.createLogWithManyChunks(filename, userId, chunkCount)

        assertThat(result).isEqualTo(logEntity)
        verify(logRepository).save(any(LogEntity::class.java))
        verify(logChunkRepository).saveAll(anyList())
    }
}
