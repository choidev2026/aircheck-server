package com.seriouschoi.aircheck.domain.port.out

/**
 * 푸시 알림 발송 Port (아웃바운드)
 * 
 * 구현체: FcmAdapter 등
 */
interface PushNotificationPort {
    fun sendPush(token: String, title: String, body: String, data: Map<String, String> = emptyMap()): Boolean
    
    fun sendWeatherSummary(
        token: String,
        temperature: Double,
        weatherCondition: String,
        pm25Grade: String,
        recommendation: String
    ): Boolean
}
