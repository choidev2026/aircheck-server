package com.seriouschoi.aircheck.controller

import com.seriouschoi.aircheck.model.AirQualityResponse
import com.seriouschoi.aircheck.model.WeatherResponse
import com.seriouschoi.aircheck.service.AirKoreaService
import com.seriouschoi.aircheck.service.WeatherService
import kotlinx.serialization.Serializable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 통합 응답 (날씨 + 대기질)
 */
@Serializable
data class CombinedWeatherResponse(
    val weather: WeatherResponse?,
    val airQuality: AirQualityResponse?
)

@RestController
@RequestMapping("/api/v1")
class WeatherController(
    private val weatherService: WeatherService,
    private val airKoreaService: AirKoreaService
) {

    /**
     * 통합 날씨/대기질 API
     * 
     * GET /api/v1/weather?lat={lat}&lng={lng}
     */
    @GetMapping("/weather")
    fun getWeather(
        @RequestParam lat: Double,
        @RequestParam lng: Double
    ): ResponseEntity<CombinedWeatherResponse> {
        val weather = weatherService.getWeather(lat, lng)
        val airQuality = airKoreaService.getAirQuality(lat, lng)
        
        return ResponseEntity.ok(CombinedWeatherResponse(weather, airQuality))
    }

    /**
     * 날씨만 조회
     * 
     * GET /api/v1/weather/forecast?lat={lat}&lng={lng}
     */
    @GetMapping("/weather/forecast")
    fun getForecast(
        @RequestParam lat: Double,
        @RequestParam lng: Double
    ): ResponseEntity<WeatherResponse?> {
        return ResponseEntity.ok(weatherService.getWeather(lat, lng))
    }

    /**
     * 대기질만 조회
     * 
     * GET /api/v1/weather/air?lat={lat}&lng={lng}
     */
    @GetMapping("/weather/air")
    fun getAirQuality(
        @RequestParam lat: Double,
        @RequestParam lng: Double
    ): ResponseEntity<AirQualityResponse?> {
        return ResponseEntity.ok(airKoreaService.getAirQuality(lat, lng))
    }
}
