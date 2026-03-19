package com.seriouschoi.aircheck.application

import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.model.WeatherResponse
import com.seriouschoi.aircheck.domain.port.`in`.CombinedWeatherResult
import com.seriouschoi.aircheck.domain.port.`in`.GetWeatherUseCase
import com.seriouschoi.aircheck.domain.port.out.AirQualityPort
import com.seriouschoi.aircheck.domain.port.out.WeatherPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class WeatherService(
    private val weatherPort: WeatherPort,
    private val airQualityPort: AirQualityPort
) : GetWeatherUseCase {

    @Cacheable("weather", key = "#lat + ',' + #lng")
    override fun getWeather(lat: Double, lng: Double): WeatherResponse? {
        return weatherPort.getWeather(lat, lng)
    }

    @Cacheable("airquality", key = "#lat + ',' + #lng")
    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        return airQualityPort.getAirQuality(lat, lng)
    }

    @Cacheable("combined", key = "#lat + ',' + #lng")
    override fun getCombined(lat: Double, lng: Double): CombinedWeatherResult {
        return CombinedWeatherResult(
            weather = weatherPort.getWeather(lat, lng),
            airQuality = airQualityPort.getAirQuality(lat, lng)
        )
    }
}
