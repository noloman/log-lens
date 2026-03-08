package me.manulorenzo.loglens.api.domain.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "log_entries")
class LogEntryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    val serviceName: String,
    val level: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
