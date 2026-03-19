package com.seriouschoi.aircheck.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ApiUsageRepository : JpaRepository<ApiUsageEntity, Long> {
    
    fun findByUsageDateAndApiType(usageDate: LocalDate, apiType: ApiType): ApiUsageEntity?
    
    fun findByUsageDateBetween(startDate: LocalDate, endDate: LocalDate): List<ApiUsageEntity>
    
    fun findByApiTypeAndUsageDateBetween(
        apiType: ApiType,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ApiUsageEntity>
    
    @Query("""
        SELECT SUM(e.callCount) FROM ApiUsageEntity e 
        WHERE e.usageDate = :date AND e.apiType = :apiType
    """)
    fun getTotalCallsByDateAndType(date: LocalDate, apiType: ApiType): Long?
    
    @Query("""
        SELECT e FROM ApiUsageEntity e 
        WHERE e.usageDate = :date
        ORDER BY e.apiType
    """)
    fun findAllByDate(date: LocalDate): List<ApiUsageEntity>
}
