package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.port.GetWeatherUseCase
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class WeatherService(
    private val weatherPort: WeatherPort,
    private val airQualityPort: AirQualityPort
) : GetWeatherUseCase {

    @Cacheable("weather", key = "#lat + ',' + #lng", unless = "#result == null")
    override fun getWeather(lat: Double, lng: Double): WeatherResponse? {
        return weatherPort.getWeather(lat, lng)
    }

    @Cacheable("airquality", key = "#lat + ',' + #lng", unless = "#result == null")
    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        return airQualityPort.getAirQuality(lat, lng)
    }

    @Cacheable("combined", key = "#lat + ',' + #lng", unless = "#result.weather == null || #result.airQuality == null")
    override fun getCombined(lat: Double, lng: Double): CombinedWeatherResult {
        return CombinedWeatherResult(
            weather = weatherPort.getWeather(lat, lng),
            airQuality = airQualityPort.getAirQuality(lat, lng)
        )
    }
}
