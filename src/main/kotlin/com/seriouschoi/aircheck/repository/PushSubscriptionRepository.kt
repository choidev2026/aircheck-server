package com.seriouschoi.aircheck.repository

import com.seriouschoi.aircheck.entity.PushSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface PushSubscriptionRepository : JpaRepository<PushSubscription, Long> {

    /** FCM 토큰으로 조회 */
    fun findByFcmToken(fcmToken: String): PushSubscription?

    /** FCM 토큰 존재 여부 */
    fun existsByFcmToken(fcmToken: String): Boolean

    /** 활성화된 구독 중 특정 시간대 조회 */
    @Query("SELECT p FROM PushSubscription p WHERE p.enabled = true AND p.pushTime = :time")
    fun findEnabledByPushTime(time: LocalTime): List<PushSubscription>

    /** 활성화된 모든 구독 조회 */
    fun findByEnabledTrue(): List<PushSubscription>
}
