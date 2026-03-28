package com.seriouschoi.aircheck.core.persistence

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "service_version")
class ServiceVersionEntity(
    @Id
    @Column(name = "os_type", length = 20)
    @Enumerated(EnumType.STRING)
    val osType: OsType,
    
    @Column(name = "min_version_code", nullable = false)
    val minVersionCode: Int,
    
    @Column(name = "update_url", length = 255)
    val updateUrl: String? = null,
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class OsType {
    ANDROID, IOS
}
