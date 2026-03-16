package com.seriouschoi.aircheck.adapter.out.persistence

import com.seriouschoi.aircheck.domain.port.out.PushSubscriptionData
import com.seriouschoi.aircheck.domain.port.out.PushSubscriptionPort
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class PushSubscriptionAdapter(
    private val repository: PushSubscriptionRepository
) : PushSubscriptionPort {

    override fun save(subscription: PushSubscriptionData): PushSubscriptionData {
        val entity = subscription.toEntity()
        val saved = repository.save(entity)
        return saved.toData()
    }

    override fun findByFcmToken(fcmToken: String): PushSubscriptionData? {
        return repository.findByFcmToken(fcmToken)?.toData()
    }

    override fun delete(subscription: PushSubscriptionData) {
        repository.deleteById(subscription.id)
    }

    override fun findEnabledByPushTime(time: LocalTime): List<PushSubscriptionData> {
        return repository.findEnabledByPushTime(time).map { it.toData() }
    }

    private fun PushSubscriptionData.toEntity() = PushSubscriptionEntity(
        id = id,
        fcmToken = fcmToken,
        latitude = latitude,
        longitude = longitude,
        address = address,
        pushTime = pushTime,
        enabled = enabled
    )

    private fun PushSubscriptionEntity.toData() = PushSubscriptionData(
        id = id,
        fcmToken = fcmToken,
        latitude = latitude,
        longitude = longitude,
        address = address,
        pushTime = pushTime,
        enabled = enabled
    )
}
