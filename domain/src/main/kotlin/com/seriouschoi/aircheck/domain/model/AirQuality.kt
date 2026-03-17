package com.seriouschoi.aircheck.domain.model

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable

/**
 * 대기질 응답 (앱으로 전달)
 * 
 * 밀도(μg/m³) 값만 반환합니다.
 * 등급은 앱에서 자체 기준(WHO 2021 등)으로 계산합니다.
 * 
 * PM10/PM25 값이 메인 측정소에 없으면 주변 측정소에서 가져오며,
 * 이 경우 pm10Station/pm25Station에 해당 측정소 정보가 표시됩니다.
 */
@Schema(description = "대기질 정보")
@Serializable
data class AirQualityResponse(
    @Schema(description = "메인 측정소 이름", example = "중구")
    val stationName: String,
    
    @Schema(description = "시도명", example = "서울")
    val sidoName: String,
    
    @Schema(description = "측정 시간", example = "2024-03-17 21:00")
    val dataTime: String,
    
    @Schema(description = "미세먼지(PM10) 밀도 (μg/m³)", example = "45")
    val pm10: Int?,
    
    @Schema(description = "PM10 측정소 (주변 측정소에서 가져온 경우)", example = "소사본동")
    val pm10Station: String? = null,
    
    @Schema(description = "초미세먼지(PM2.5) 밀도 (μg/m³)", example = "23")
    val pm25: Int?,
    
    @Schema(description = "PM2.5 측정소 (주변 측정소에서 가져온 경우)", example = "소사본동")
    val pm25Station: String? = null
)
