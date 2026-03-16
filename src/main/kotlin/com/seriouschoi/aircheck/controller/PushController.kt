package com.seriouschoi.aircheck.controller

import com.seriouschoi.aircheck.service.PushService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalTime

// ── Request DTOs ───────────────────────────────────────────────────────────

data class SubscribeRequest(
    @field:NotBlank
    val fcmToken: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val pushTimeHour: Int = 7,
    val pushTimeMinute: Int = 0,
    val enabled: Boolean = true
)

data class UnsubscribeRequest(
    @field:NotBlank
    val fcmToken: String
)

data class SetEnabledRequest(
    @field:NotBlank
    val fcmToken: String,
    val enabled: Boolean
)

// ── Response DTOs ──────────────────────────────────────────────────────────

data class SubscriptionResponse(
    val success: Boolean,
    val message: String,
    val pushTime: String? = null
)

// ── Controller ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/push")
class PushController(
    private val pushService: PushService
) {

    /**
     * 푸시 알림 구독
     * 
     * POST /api/v1/push/subscribe
     */
    @PostMapping("/subscribe")
    fun subscribe(@Valid @RequestBody request: SubscribeRequest): ResponseEntity<SubscriptionResponse> {
        val pushTime = LocalTime.of(request.pushTimeHour, request.pushTimeMinute)
        
        val subscription = pushService.subscribe(
            fcmToken = request.fcmToken,
            latitude = request.latitude,
            longitude = request.longitude,
            address = request.address,
            pushTime = pushTime,
            enabled = request.enabled
        )

        return ResponseEntity.ok(
            SubscriptionResponse(
                success = true,
                message = "푸시 알림이 등록되었습니다.",
                pushTime = subscription.pushTime.toString()
            )
        )
    }

    /**
     * 푸시 알림 구독 해제
     * 
     * POST /api/v1/push/unsubscribe
     */
    @PostMapping("/unsubscribe")
    fun unsubscribe(@Valid @RequestBody request: UnsubscribeRequest): ResponseEntity<SubscriptionResponse> {
        val result = pushService.unsubscribe(request.fcmToken)
        
        return ResponseEntity.ok(
            SubscriptionResponse(
                success = result,
                message = if (result) "구독이 해제되었습니다." else "구독 정보를 찾을 수 없습니다."
            )
        )
    }

    /**
     * 푸시 알림 활성화/비활성화
     * 
     * POST /api/v1/push/enabled
     */
    @PostMapping("/enabled")
    fun setEnabled(@Valid @RequestBody request: SetEnabledRequest): ResponseEntity<SubscriptionResponse> {
        val subscription = pushService.setEnabled(request.fcmToken, request.enabled)
        
        return ResponseEntity.ok(
            SubscriptionResponse(
                success = subscription != null,
                message = if (subscription != null) {
                    if (request.enabled) "알림이 활성화되었습니다." else "알림이 비활성화되었습니다."
                } else {
                    "구독 정보를 찾을 수 없습니다."
                }
            )
        )
    }
}
