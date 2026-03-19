package com.seriouschoi.aircheck.adapter.out.persistence

import com.seriouschoi.aircheck.application.port.out.ApiUsagePort
import com.seriouschoi.aircheck.application.port.out.ApiUsageStats
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ApiUsageAdapter(
    private val repository: ApiUsageRepository
) : ApiUsagePort {
    
    @Transactional
    override fun recordSuccess(apiType: String, responseTimeMs: Long) {
        val type = ApiType.valueOf(apiType)
        val today = LocalDate.now()
        
        val entity = repository.findByUsageDateAndApiType(today, type)
            ?: ApiUsageEntity(usageDate = today, apiType = type)
        
        entity.incrementSuccess(responseTimeMs)
        repository.save(entity)
    }
    
    @Transactional
    override fun recordFailure(apiType: String) {
        val type = ApiType.valueOf(apiType)
        val today = LocalDate.now()
        
        val entity = repository.findByUsageDateAndApiType(today, type)
            ?: ApiUsageEntity(usageDate = today, apiType = type)
        
        entity.incrementFail()
        repository.save(entity)
    }
    
    @Transactional(readOnly = true)
    override fun getTodayCount(apiType: String): Long {
        val type = ApiType.valueOf(apiType)
        return repository.getTotalCallsByDateAndType(LocalDate.now(), type) ?: 0
    }
    
    @Transactional(readOnly = true)
    override fun getTodayStats(): List<ApiUsageStats> {
        return repository.findAllByDate(LocalDate.now()).map { entity ->
            ApiUsageStats(
                apiType = entity.apiType.name,
                date = entity.usageDate,
                callCount = entity.callCount,
                successCount = entity.successCount,
                failCount = entity.failCount,
                avgResponseTimeMs = entity.avgResponseTimeMs
            )
        }
    }
    
    @Transactional(readOnly = true)
    override fun getStats(startDate: LocalDate, endDate: LocalDate): List<ApiUsageStats> {
        return repository.findByUsageDateBetween(startDate, endDate).map { entity ->
            ApiUsageStats(
                apiType = entity.apiType.name,
                date = entity.usageDate,
                callCount = entity.callCount,
                successCount = entity.successCount,
                failCount = entity.failCount,
                avgResponseTimeMs = entity.avgResponseTimeMs
            )
        }
    }
}
