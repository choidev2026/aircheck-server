package com.seriouschoi.aircheck.domain.model

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable

/**
 * 대기질 응답 (앱으로 전달)
 * 
 * 밀도(μg/m³) 값과 등급을 함께 반환합니다.
 * 앱에서 밀도 값을 기반으로 자체 기준(WHO 등)으로 등급을 재계산할 수 있습니다.
 */
@Schema(description = "대기질 정보")
@Serializable
data class AirQualityResponse(
    @Schema(description = "측정소 이름", example = "중구")
    val stationName: String,
    
    @Schema(description = "시도명", example = "서울")
    val sidoName: String,
    
    @Schema(description = "측정 시간", example = "2024-03-17 21:00")
    val dataTime: String,
    
    @Schema(description = "미세먼지(PM10) 밀도 (μg/m³)", example = "45")
    val pm10: Int?,
    
    @Schema(description = "초미세먼지(PM2.5) 밀도 (μg/m³)", example = "23")
    val pm25: Int?,
    
    @Schema(description = "통합대기환경지수 (CAI)", example = "75")
    val aqi: Int?,
    
    @Schema(description = "PM10 등급 (한국 환경부 기준)")
    val pm10Grade: AirGrade,
    
    @Schema(description = "PM2.5 등급 (한국 환경부 기준)")
    val pm25Grade: AirGrade,
    
    @Schema(description = "통합대기환경지수 등급")
    val aqiGrade: AirGrade,
    
    @Schema(description = "최악 등급 (PM10, PM2.5, AQI 중 가장 나쁜 등급)")
    val worstGrade: AirGrade
)

/**
 * 대기질 등급 (8단계 한국 기준)
 * 
 * 에어코리아 통합대기환경지수(CAI) 기준
 */
@Schema(description = "대기질 등급", enumAsRef = true)
@Serializable
enum class AirGrade(
    @Schema(description = "등급 라벨")
    val label: String,
    @Schema(description = "이모지")
    val emoji: String,
    @Schema(description = "등급 레벨 (1=최고, 8=최악)")
    val level: Int
) {
    BEST("최고", "🌟", 1),
    GOOD("좋음", "😊", 2),
    FAIR("양호", "🙂", 3),
    MODERATE("보통", "😐", 4),
    BAD("나쁨", "😮", 5),
    POOR("상당히나쁨", "😷", 6),
    VERY_BAD("매우나쁨", "😱", 7),
    WORST("최악", "💀", 8);

    companion object {
        /** 
         * PM2.5 밀도(μg/m³)를 등급으로 변환
         * 한국 환경부 기준
         */
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

        /** 
         * PM10 밀도(μg/m³)를 등급으로 변환
         * 한국 환경부 기준
         */
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

        /** 
         * 통합대기환경지수(CAI)를 등급으로 변환
         */
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

        /** 여러 등급 중 최악 반환 */
        fun worst(vararg grades: AirGrade): AirGrade = grades.maxByOrNull { it.level } ?: MODERATE
    }
}
