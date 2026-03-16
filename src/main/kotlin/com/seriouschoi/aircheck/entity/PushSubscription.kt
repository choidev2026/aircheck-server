package com.seriouschoi.aircheck.entity

import jakarta.persistence.*
import java.time.LocalTime

/**
 * 푸시 알림 구독 정보
 */
@Entity
@Table(name = "push_subscriptions")
data class PushSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** FCM 디바이스 토큰 */
    @Column(nullable = false, unique = true, length = 512)
    val fcmToken: String,

    /** 알림 받을 위치 - 위도 */
    @Column(nullable = false)
    val latitude: Double,

    /** 알림 받을 위치 - 경도 */
    @Column(nullable = false)
    val longitude: Double,

    /** 알림 받을 위치 - 주소 (표시용) */
    @Column(length = 200)
    val address: String? = null,

    /** 알림 시간 (예: 07:00) */
    @Column(nullable = false)
    val pushTime: LocalTime = LocalTime.of(7, 0),

    /** 알림 활성화 여부 */
    @Column(nullable = false)
    val enabled: Boolean = true,

    /** 생성일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),

    /** 수정일시 */
    @Column(nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
