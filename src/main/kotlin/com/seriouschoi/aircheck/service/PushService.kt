package com.seriouschoi.aircheck.service

import com.seriouschoi.aircheck.entity.PushSubscription
import com.seriouschoi.aircheck.repository.PushSubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class PushService(
    private val pushRepository: PushSubscriptionRepository,
    private val fcmService: FcmService,
    private val weatherService: WeatherService,
    private val airKoreaService: AirKoreaService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 푸시 구독 등록/수정
     */
    @Transactional
    fun subscribe(
        fcmToken: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        pushTime: LocalTime,
        enabled: Boolean = true
    ): PushSubscription {
        val existing = pushRepository.findByFcmToken(fcmToken)
        
        val subscription = existing?.copy(
            latitude = latitude,
            longitude = longitude,
            address = address,
            pushTime = pushTime,
            enabled = enabled,
            updatedAt = java.time.LocalDateTime.now()
        ) ?: PushSubscription(
            fcmToken = fcmToken,
            latitude = latitude,
            longitude = longitude,
            address = address,
            pushTime = pushTime,
            enabled = enabled
        )
        
        return pushRepository.save(subscription)
    }

    /**
     * 푸시 구독 해제
     */
    @Transactional
    fun unsubscribe(fcmToken: String): Boolean {
        val subscription = pushRepository.findByFcmToken(fcmToken) ?: return false
        pushRepository.delete(subscription)
        return true
    }

    /**
     * 푸시 활성화/비활성화
     */
    @Transactional
    fun setEnabled(fcmToken: String, enabled: Boolean): PushSubscription? {
        val subscription = pushRepository.findByFcmToken(fcmToken) ?: return null
        return pushRepository.save(subscription.copy(enabled = enabled, updatedAt = java.time.LocalDateTime.now()))
    }

    /**
     * 특정 시간에 알림 받을 구독자들에게 푸시 발송
     */
    fun sendScheduledPush(time: LocalTime) {
        val subscriptions = pushRepository.findEnabledByPushTime(time)
        log.info("Sending scheduled push for $time to ${subscriptions.size} subscribers")

        subscriptions.forEach { sub ->
            try {
                // 날씨 & 대기질 조회
                val weather = weatherService.getWeather(sub.latitude, sub.longitude)
                val air = airKoreaService.getAirQuality(sub.latitude, sub.longitude)

                if (weather != null) {
                    val recommendation = buildRecommendation(weather.current.temperature, air?.pm25)
                    
                    fcmService.sendWeatherSummary(
                        token = sub.fcmToken,
                        temperature = weather.current.temperature,
                        weatherCondition = weather.current.weatherCondition.emoji + " " + weather.current.weatherCondition.label,
                        pm25Grade = air?.pm25Grade?.emoji + " " + (air?.pm25Grade?.label ?: "알 수 없음"),
                        recommendation = recommendation
                    )
                }
            } catch (e: Exception) {
                log.error("Failed to send push to ${sub.id}: ${e.message}")
            }
        }
    }

    private fun buildRecommendation(temp: Double, pm25: Int?): String {
        val clothes = when {
            temp >= 28 -> "민소매/반바지"
            temp >= 23 -> "반팔/얇은 옷"
            temp >= 17 -> "긴팔/가디건"
            temp >= 12 -> "자켓/니트"
            temp >= 6 -> "코트/히트텍"
            temp >= 0 -> "패딩"
            else -> "두꺼운 패딩"
        }

        val mask = if ((pm25 ?: 0) > 35) "마스크 챙기세요!" else ""
        
        return "👕 $clothes $mask".trim()
    }
}
