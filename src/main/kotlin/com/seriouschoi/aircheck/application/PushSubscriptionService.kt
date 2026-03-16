package com.seriouschoi.aircheck.application

import com.seriouschoi.aircheck.domain.port.`in`.GetWeatherUseCase
import com.seriouschoi.aircheck.domain.port.`in`.PushSubscriptionResult
import com.seriouschoi.aircheck.domain.port.`in`.PushSubscriptionUseCase
import com.seriouschoi.aircheck.domain.port.out.PushNotificationPort
import com.seriouschoi.aircheck.adapter.out.persistence.PushSubscriptionEntity
import com.seriouschoi.aircheck.adapter.out.persistence.PushSubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class PushSubscriptionService(
    private val pushRepository: PushSubscriptionRepository,
    private val pushNotificationPort: PushNotificationPort,
    private val weatherUseCase: GetWeatherUseCase
) : PushSubscriptionUseCase {
    
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun subscribe(
        fcmToken: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        pushTime: LocalTime,
        enabled: Boolean
    ): PushSubscriptionResult {
        val existing = pushRepository.findByFcmToken(fcmToken)
        
        val entity = existing?.copy(
            latitude = latitude,
            longitude = longitude,
            address = address,
            pushTime = pushTime,
            enabled = enabled,
            updatedAt = java.time.LocalDateTime.now()
        ) ?: PushSubscriptionEntity(
            fcmToken = fcmToken,
            latitude = latitude,
            longitude = longitude,
            address = address,
            pushTime = pushTime,
            enabled = enabled
        )
        
        val saved = pushRepository.save(entity)
        return saved.toResult()
    }

    @Transactional
    override fun unsubscribe(fcmToken: String): Boolean {
        val subscription = pushRepository.findByFcmToken(fcmToken) ?: return false
        pushRepository.delete(subscription)
        return true
    }

    @Transactional
    override fun setEnabled(fcmToken: String, enabled: Boolean): PushSubscriptionResult? {
        val subscription = pushRepository.findByFcmToken(fcmToken) ?: return null
        val updated = pushRepository.save(
            subscription.copy(enabled = enabled, updatedAt = java.time.LocalDateTime.now())
        )
        return updated.toResult()
    }

    override fun sendScheduledPush(time: LocalTime) {
        val subscriptions = pushRepository.findEnabledByPushTime(time)
        log.info("Sending scheduled push for $time to ${subscriptions.size} subscribers")

        subscriptions.forEach { sub ->
            try {
                val weather = weatherUseCase.getWeather(sub.latitude, sub.longitude)
                val air = weatherUseCase.getAirQuality(sub.latitude, sub.longitude)

                if (weather != null) {
                    val recommendation = buildRecommendation(weather.current.temperature, air?.pm25)
                    
                    pushNotificationPort.sendWeatherSummary(
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

    private fun PushSubscriptionEntity.toResult() = PushSubscriptionResult(
        id = id,
        fcmToken = fcmToken,
        pushTime = pushTime,
        enabled = enabled
    )
}
