package com.seriouschoi.aircheck.adapter.out.persistence

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 푸시 알림 구독 정보 Entity
 */
@Entity
@Table(name = "push_subscriptions")
data class PushSubscriptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 512)
    val fcmToken: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @Column(length = 200)
    val address: String? = null,

    @Column(nullable = false)
    val pushTime: LocalTime = LocalTime.of(7, 0),

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
