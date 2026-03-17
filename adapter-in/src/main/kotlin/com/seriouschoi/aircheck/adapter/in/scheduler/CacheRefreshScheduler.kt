package com.seriouschoi.aircheck.adapter.`in`.scheduler

import com.seriouschoi.aircheck.domain.port.out.AirQualityPort
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CacheRefreshScheduler(
    private val airQualityPort: AirQualityPort,
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("측정소 정보 로딩 시작...")
        airQualityPort.loadStationCoordinates()
        log.info("측정소 정보 로딩 완료")
    }

    @Scheduled(fixedRate = 600_000) // 10분
    fun logCacheStats() {
        val airCache = cacheManager.getCache("airquality")
        val weatherCache = cacheManager.getCache("weather")
        log.debug("캐시 상태 - airquality: ${airCache != null}, weather: ${weatherCache != null}")
    }

    @Scheduled(fixedRate = 21_600_000) // 6시간
    fun refreshStationList() {
        log.info("측정소 목록 갱신 중...")
        airQualityPort.loadStationCoordinates()
        log.info("측정소 목록 갱신 완료")
    }
}
