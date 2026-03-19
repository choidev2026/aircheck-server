package com.seriouschoi.aircheck.core.domain.model

import kotlinx.serialization.Serializable

/**
 * 날씨 응답 (앱으로 전달)
 */
@Serializable
data class WeatherResponse(
    val current: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val precipitation: Double,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition,
    val cloudCover: Int,
    val isDay: Boolean
)

@Serializable
data class HourlyForecast(
    val time: String,
    val hour: Int,
    val temperature: Double,
    val feelsLike: Double,
    val precipitationProbability: Int,
    val snowfall: Double,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition
)

@Serializable
enum class WeatherCondition(val label: String, val emoji: String) {
    CLEAR("맑음", "☀️"),
    PARTLY_CLOUDY("구름 조금", "⛅"),
    CLOUDY("흐림", "☁️"),
    FOG("안개", "🌫️"),
    DRIZZLE("이슬비", "🌧️"),
    RAIN("비", "🌧️"),
    HEAVY_RAIN("폭우", "⛈️"),
    SNOW("눈", "🌨️"),
    HEAVY_SNOW("폭설", "❄️"),
    THUNDERSTORM("뇌우", "⛈️");

    companion object {
        fun fromWeatherCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            61, 63, 80, 81 -> RAIN
            65, 82 -> HEAVY_RAIN
            71, 73, 85 -> SNOW
            75, 86 -> HEAVY_SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}
