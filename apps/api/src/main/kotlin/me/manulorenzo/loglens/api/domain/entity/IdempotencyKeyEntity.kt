package me.manulorenzo.loglens.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKeyEntity(
    @Id
    @Column(name = "key_id")
    val key: String,
    @Column(name = "response", columnDefinition = "bytea")
    val response: ByteArray,
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
