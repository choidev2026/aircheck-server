package com.seriouschoi.aircheck.domain.port.`in`

import java.time.LocalTime

/**
 * 푸시 구독 관리 UseCase (인바운드 Port)
 */
interface PushSubscriptionUseCase {
    fun subscribe(
        fcmToken: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        pushTime: LocalTime,
        enabled: Boolean = true
    ): PushSubscriptionResult

    fun unsubscribe(fcmToken: String): Boolean
    fun setEnabled(fcmToken: String, enabled: Boolean): PushSubscriptionResult?
    fun sendScheduledPush(time: LocalTime)
}

data class PushSubscriptionResult(
    val id: Long,
    val fcmToken: String,
    val pushTime: LocalTime,
    val enabled: Boolean
)
