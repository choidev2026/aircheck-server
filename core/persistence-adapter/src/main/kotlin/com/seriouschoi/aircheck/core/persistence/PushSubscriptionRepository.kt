package com.seriouschoi.aircheck.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface PushSubscriptionRepository : JpaRepository<PushSubscriptionEntity, Long> {

    fun findByFcmToken(fcmToken: String): PushSubscriptionEntity?

    fun existsByFcmToken(fcmToken: String): Boolean

    @Query("SELECT p FROM PushSubscriptionEntity p WHERE p.enabled = true AND p.pushTime = :time")
    fun findEnabledByPushTime(time: LocalTime): List<PushSubscriptionEntity>

    fun findByEnabledTrue(): List<PushSubscriptionEntity>
}
