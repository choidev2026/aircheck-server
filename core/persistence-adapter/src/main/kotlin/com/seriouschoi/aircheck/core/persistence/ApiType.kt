package com.seriouschoi.aircheck.core.persistence

enum class ApiType {
    OPEN_METEO,
    AIR_KOREA,
    KMA_ULTRA_SRT_NCST,   // 초단기실황
    KMA_ULTRA_SRT_FCST,   // 초단기예보
    KMA_VILAGE_FCST,      // 단기예보
    KMA_MID_FCST          // 중기예보
}
