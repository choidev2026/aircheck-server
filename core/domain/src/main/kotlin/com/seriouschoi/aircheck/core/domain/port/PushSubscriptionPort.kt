package com.seriouschoi.aircheck.core.domain.port

import java.time.LocalTime

/**
 * 푸시 구독 저장소 Port (아웃바운드)
 */
interface PushSubscriptionPort {
    fun save(subscription: PushSubscriptionData): PushSubscriptionData
    fun findByFcmToken(fcmToken: String): PushSubscriptionData?
    fun delete(subscription: PushSubscriptionData)
    fun findEnabledByPushTime(time: LocalTime): List<PushSubscriptionData>
}

/**
 * 푸시 구독 데이터 (도메인 모델)
 */
data class PushSubscriptionData(
    val id: Long = 0,
    val fcmToken: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val pushTime: LocalTime,
    val enabled: Boolean = true
)
