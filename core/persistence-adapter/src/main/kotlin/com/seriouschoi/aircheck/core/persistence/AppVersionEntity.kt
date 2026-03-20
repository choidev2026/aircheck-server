package com.seriouschoi.aircheck.core.persistence

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "app_versions")
class AppVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val platform: Platform,
    
    @Column(name = "min_version", nullable = false, length = 20)
    val minVersion: String,
    
    @Column(name = "latest_version", nullable = false, length = 20)
    val latestVersion: String,
    
    @Column(name = "force_update", nullable = false)
    val forceUpdate: Boolean = false,
    
    @Column(name = "update_url", length = 255)
    val updateUrl: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val message: String? = null,
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class Platform {
    ANDROID, IOS
}
