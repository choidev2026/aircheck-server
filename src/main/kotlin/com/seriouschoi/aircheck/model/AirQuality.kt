package com.seriouschoi.aircheck.model

import kotlinx.serialization.Serializable

/**
 * 대기질 응답 (앱으로 전달)
 */
@Serializable
data class AirQualityResponse(
    val stationName: String,
    val sidoName: String,
    val dataTime: String,
    val pm10: Int?,
    val pm25: Int?,
    val aqi: Int?,
    val pm10Grade: AirGrade,
    val pm25Grade: AirGrade,
    val aqiGrade: AirGrade,
    val worstGrade: AirGrade
)

/**
 * 대기질 등급 (8단계 한국 기준)
 */
@Serializable
enum class AirGrade(val label: String, val emoji: String, val level: Int) {
    BEST("최고", "🌟", 1),
    GOOD("좋음", "😊", 2),
    FAIR("양호", "🙂", 3),
    MODERATE("보통", "😐", 4),
    BAD("나쁨", "😮", 5),
    POOR("상당히나쁨", "😷", 6),
    VERY_BAD("매우나쁨", "😱", 7),
    WORST("최악", "💀", 8);

    companion object {
        fun fromPm25(value: Int?): AirGrade = when (value) {
            null -> MODERATE
            in 0..8 -> BEST
            in 9..15 -> GOOD
            in 16..20 -> FAIR
            in 21..25 -> MODERATE
            in 26..35 -> BAD
            in 36..50 -> POOR
            in 51..75 -> VERY_BAD
            else -> WORST
        }

        fun fromPm10(value: Int?): AirGrade = when (value) {
            null -> MODERATE
            in 0..15 -> BEST
            in 16..30 -> GOOD
            in 31..40 -> FAIR
            in 41..50 -> MODERATE
            in 51..75 -> BAD
            in 76..100 -> POOR
            in 101..150 -> VERY_BAD
            else -> WORST
        }

        fun fromAqi(value: Int?): AirGrade = when (value) {
            null -> MODERATE
            in 0..25 -> BEST
            in 26..50 -> GOOD
            in 51..75 -> FAIR
            in 76..100 -> MODERATE
            in 101..150 -> BAD
            in 151..200 -> POOR
            in 201..300 -> VERY_BAD
            else -> WORST
        }

        fun worst(vararg grades: AirGrade): AirGrade = grades.maxByOrNull { it.level } ?: MODERATE
    }
}
