package me.manulorenzo.loglens.api.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "logs")
class LogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    var fileName: String,
    val userId: UUID,
    val uploadedAt: Instant,
    @Version
    val version: Long? = null,
    @OneToMany(mappedBy = "log", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val chunks: MutableList<LogChunkEntity> = mutableListOf(),
)
