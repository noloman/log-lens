package me.manulorenzo.loglens.api.domain.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "log_chunk")
class LogChunkEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    val content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id")
    val log: LogEntity,
)
