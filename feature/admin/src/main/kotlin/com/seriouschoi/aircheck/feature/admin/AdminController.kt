package com.seriouschoi.aircheck.feature.admin

import com.seriouschoi.aircheck.core.service.port.ApiUsagePort
import com.seriouschoi.aircheck.core.service.port.ApiUsageStats
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@RestController
@RequestMapping("/admin")
class AdminController(
    private val apiUsagePort: ApiUsagePort,
    @Value("\${admin.api-key}") private val adminApiKey: String
) {
    
    private fun validateApiKey(key: String?) {
        if (key != adminApiKey) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key")
        }
    }
    
    /**
     * 오늘 API 사용량 조회
     */
    @GetMapping("/api-usage/today")
    fun getTodayUsage(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): ApiUsageResponse {
        validateApiKey(apiKey)
        val stats = apiUsagePort.getTodayStats()
        return ApiUsageResponse(
            date = LocalDate.now(),
            stats = stats,
            limits = API_LIMITS
        )
    }
    
    /**
     * 기간별 API 사용량 조회
     */
    @GetMapping("/api-usage")
    fun getUsage(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        endDate: LocalDate
    ): List<ApiUsageStats> {
        validateApiKey(apiKey)
        return apiUsagePort.getStats(startDate, endDate)
    }
    
    /**
     * API 잔여 호출 수 조회
     */
    @GetMapping("/api-usage/remaining")
    fun getRemainingCalls(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): Map<String, RemainingCalls> {
        validateApiKey(apiKey)
        val stats = apiUsagePort.getTodayStats()
        
        return API_LIMITS.mapValues { (apiType, limit) ->
            val used = stats.find { it.apiType == apiType }?.callCount ?: 0
            RemainingCalls(
                limit = limit,
                used = used,
                remaining = limit - used,
                usagePercent = if (limit > 0) (used.toDouble() / limit * 100) else 0.0
            )
        }
    }
    
    /**
     * 시간대별 통계 조회
     */
    @GetMapping("/api-usage/hourly")
    fun getHourlyStats(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): List<com.seriouschoi.aircheck.core.service.port.HourlyStats> {
        validateApiKey(apiKey)
        return apiUsagePort.getHourlyStats(date ?: LocalDate.now())
    }
    
    /**
     * 수동 로그 정리
     */
    @DeleteMapping("/api-usage/cleanup")
    fun cleanupLogs(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam(defaultValue = "30") retentionDays: Int
    ): Map<String, Any> {
        validateApiKey(apiKey)
        val deleted = apiUsagePort.cleanupOldLogs(retentionDays)
        return mapOf(
            "deletedCount" to deleted,
            "retentionDays" to retentionDays
        )
    }
    
    companion object {
        val API_LIMITS = mapOf(
            "OPEN_METEO" to 10_000L,
            "AIR_KOREA" to 500L,
            "KMA_ULTRA_SHORT" to 10_000L
        )
    }
}

data class ApiUsageResponse(
    val date: LocalDate,
    val stats: List<ApiUsageStats>,
    val limits: Map<String, Long>
)

data class RemainingCalls(
    val limit: Long,
    val used: Long,
    val remaining: Long,
    val usagePercent: Double
)
