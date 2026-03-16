package com.seriouschoi.aircheck.scheduler

import com.seriouschoi.aircheck.service.AirKoreaService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CacheRefreshScheduler(
    private val airKoreaService: AirKoreaService,
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 서버 시작 시 측정소 정보 로드
     */
    @PostConstruct
    fun init() {
        log.info("측정소 정보 로딩 시작...")
        airKoreaService.loadStationCoordinates()
        log.info("측정소 정보 로딩 완료")
    }

    /**
     * 매 10분마다 캐시 통계 로깅
     */
    @Scheduled(fixedRate = 600_000) // 10분
    fun logCacheStats() {
        val airCache = cacheManager.getCache("airquality")
        val weatherCache = cacheManager.getCache("weather")
        log.info("캐시 상태 - airquality: ${airCache != null}, weather: ${weatherCache != null}")
    }

    /**
     * 매 6시간마다 측정소 목록 갱신
     */
    @Scheduled(fixedRate = 21_600_000) // 6시간
    fun refreshStationList() {
        log.info("측정소 목록 갱신 중...")
        airKoreaService.loadStationCoordinates()
        log.info("측정소 목록 갱신 완료")
    }
}
