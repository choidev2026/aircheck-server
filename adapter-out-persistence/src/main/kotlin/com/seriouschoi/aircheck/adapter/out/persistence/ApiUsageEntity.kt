package com.seriouschoi.aircheck.adapter.out.persistence

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "api_usage",
    indexes = [
        Index(name = "idx_api_usage_date_type", columnList = "usageDate, apiType")
    ]
)
class ApiUsageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val usageDate: LocalDate,
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val apiType: ApiType,
    
    @Column(nullable = false)
    var callCount: Long = 0,
    
    @Column(nullable = false)
    var successCount: Long = 0,
    
    @Column(nullable = false)
    var failCount: Long = 0,
    
    @Column(nullable = false)
    var totalResponseTimeMs: Long = 0,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun incrementSuccess(responseTimeMs: Long) {
        callCount++
        successCount++
        totalResponseTimeMs += responseTimeMs
        updatedAt = LocalDateTime.now()
    }
    
    fun incrementFail() {
        callCount++
        failCount++
        updatedAt = LocalDateTime.now()
    }
    
    val avgResponseTimeMs: Long
        get() = if (successCount > 0) totalResponseTimeMs / successCount else 0
}

enum class ApiType {
    OPEN_METEO,
    AIR_KOREA,
    KMA_ULTRA_SHORT  // 기상청 초단기
}
