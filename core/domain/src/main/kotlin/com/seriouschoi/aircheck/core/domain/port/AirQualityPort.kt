package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse

/**
 * 대기질 데이터 조회 Port (아웃바운드)
 * 
 * 구현체: AirKoreaAdapter 등
 */
interface AirQualityPort {
    fun getAirQuality(lat: Double, lng: Double): AirQualityResponse?
}
