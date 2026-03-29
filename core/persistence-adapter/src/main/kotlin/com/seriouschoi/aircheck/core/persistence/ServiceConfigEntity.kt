package com.seriouschoi.aircheck.core.persistence

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "service_config")
class ServiceConfigEntity(
    @Id
    @Column(name = "config_key", length = 50)
    val configKey: String,
    
    @Column(name = "config_value", nullable = false, length = 255)
    var configValue: String,
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(value: String) {
        this.configValue = value
        this.updatedAt = LocalDateTime.now()
    }
}
