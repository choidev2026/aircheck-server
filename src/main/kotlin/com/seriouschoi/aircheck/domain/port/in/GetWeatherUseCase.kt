package com.seriouschoi.aircheck.domain.port.`in`

import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.model.WeatherResponse

/**
 * 날씨/대기질 조회 UseCase (인바운드 Port)
 */
interface GetWeatherUseCase {
    fun getWeather(lat: Double, lng: Double): WeatherResponse?
    fun getAirQuality(lat: Double, lng: Double): AirQualityResponse?
    fun getCombined(lat: Double, lng: Double): CombinedWeatherResult
}

data class CombinedWeatherResult(
    val weather: WeatherResponse?,
    val airQuality: AirQualityResponse?
)
