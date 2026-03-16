package com.seriouschoi.aircheck.domain.port.out

import com.seriouschoi.aircheck.domain.model.WeatherResponse

/**
 * 날씨 데이터 조회 Port (아웃바운드)
 * 
 * 구현체: OpenMeteoAdapter, 기상청Adapter 등
 */
interface WeatherPort {
    fun getWeather(lat: Double, lng: Double): WeatherResponse?
}
