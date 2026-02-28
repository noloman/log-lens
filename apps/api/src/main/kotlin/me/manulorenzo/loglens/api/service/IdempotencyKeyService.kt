package me.manulorenzo.loglens.api.service

import me.manulorenzo.loglens.api.domain.entity.IdempotencyKeyEntity
import me.manulorenzo.loglens.api.domain.repository.IdempotencyKeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IdempotencyKeyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
) {
    @Transactional(readOnly = true)
    fun getResponse(key: String): ByteArray? =
        idempotencyKeyRepository.findById(key).map { it.response }.orElse(null)

    @Transactional
    fun saveResponse(key: String, response: ByteArray) {
        idempotencyKeyRepository.save(IdempotencyKeyEntity(key = key, response = response))
    }
}
