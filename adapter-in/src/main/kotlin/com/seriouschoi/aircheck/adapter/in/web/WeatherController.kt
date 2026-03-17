package com.seriouschoi.aircheck.adapter.`in`.web

import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.model.WeatherResponse
import com.seriouschoi.aircheck.domain.port.`in`.CombinedWeatherResult
import com.seriouschoi.aircheck.domain.port.`in`.GetWeatherUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Weather", description = "날씨 및 대기질 API")
@RestController
@RequestMapping("/api/v1")
class WeatherController(
    private val weatherUseCase: GetWeatherUseCase
) {

    @Operation(
        summary = "통합 날씨/대기질 조회",
        description = "좌표 기반으로 날씨와 대기질 정보를 함께 조회합니다. " +
                "미세먼지는 밀도(μg/m³) 값과 등급을 함께 반환합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = CombinedWeatherResult::class))]
        )
    ])
    @GetMapping("/weather")
    fun getWeather(
        @Parameter(description = "위도 (예: 37.5665)", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도 (예: 126.9780)", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<CombinedWeatherResult> {
        return ResponseEntity.ok(weatherUseCase.getCombined(lat, lng))
    }

    @Operation(
        summary = "날씨 예보 조회",
        description = "좌표 기반으로 현재 날씨와 48시간 예보를 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = WeatherResponse::class))]
        )
    ])
    @GetMapping("/weather/forecast")
    fun getForecast(
        @Parameter(description = "위도", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<WeatherResponse?> {
        return ResponseEntity.ok(weatherUseCase.getWeather(lat, lng))
    }

    @Operation(
        summary = "대기질 조회",
        description = "좌표 기반으로 대기질 정보를 조회합니다. " +
                "PM2.5, PM10 밀도(μg/m³)와 등급을 반환합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = AirQualityResponse::class))]
        )
    ])
    @GetMapping("/weather/air")
    fun getAirQuality(
        @Parameter(description = "위도", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<AirQualityResponse?> {
        return ResponseEntity.ok(weatherUseCase.getAirQuality(lat, lng))
    }
}
