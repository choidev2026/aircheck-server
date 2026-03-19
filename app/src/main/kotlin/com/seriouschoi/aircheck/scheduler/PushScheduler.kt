package com.seriouschoi.aircheck.scheduler

import com.seriouschoi.aircheck.core.domain.port.PushSubscriptionUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class PushScheduler(
    private val pushUseCase: PushSubscriptionUseCase
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매 정시마다 해당 시간에 알림 받을 구독자들에게 푸시 발송
     */
    @Scheduled(cron = "0 0 * * * *") // 매 정시
    fun sendScheduledPush() {
        val now = LocalTime.now().withSecond(0).withNano(0)
        log.info("Running scheduled push for $now")
        
        try {
            pushUseCase.sendScheduledPush(now)
        } catch (e: Exception) {
            log.error("Scheduled push failed: ${e.message}")
        }
    }
}
