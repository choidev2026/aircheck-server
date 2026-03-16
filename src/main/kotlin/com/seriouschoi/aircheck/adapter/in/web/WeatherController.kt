package com.seriouschoi.aircheck.adapter.`in`.web

import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.model.WeatherResponse
import com.seriouschoi.aircheck.domain.port.`in`.CombinedWeatherResult
import com.seriouschoi.aircheck.domain.port.`in`.GetWeatherUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class WeatherController(
    private val weatherUseCase: GetWeatherUseCase
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
    ): ResponseEntity<CombinedWeatherResult> {
        return ResponseEntity.ok(weatherUseCase.getCombined(lat, lng))
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
        return ResponseEntity.ok(weatherUseCase.getWeather(lat, lng))
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
        return ResponseEntity.ok(weatherUseCase.getAirQuality(lat, lng))
    }
}
