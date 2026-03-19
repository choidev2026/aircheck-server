package com.seriouschoi.aircheck.adapter.`in`.web

import com.seriouschoi.aircheck.application.port.out.ApiUsagePort
import com.seriouschoi.aircheck.application.port.out.ApiUsageStats
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/admin")
class AdminController(
    private val apiUsagePort: ApiUsagePort
) {
    
    /**
     * 오늘 API 사용량 조회
     */
    @GetMapping("/api-usage/today")
    fun getTodayUsage(): ApiUsageResponse {
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        endDate: LocalDate
    ): List<ApiUsageStats> {
        return apiUsagePort.getStats(startDate, endDate)
    }
    
    /**
     * API 잔여 호출 수 조회
     */
    @GetMapping("/api-usage/remaining")
    fun getRemainingCalls(): Map<String, RemainingCalls> {
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
