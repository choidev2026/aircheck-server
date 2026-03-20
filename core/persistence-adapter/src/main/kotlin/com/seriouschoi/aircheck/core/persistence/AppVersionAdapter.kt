package com.seriouschoi.aircheck.core.persistence

import com.seriouschoi.aircheck.core.domain.model.AppVersion
import com.seriouschoi.aircheck.core.domain.port.AppVersionPort
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AppVersionAdapter(
    private val repository: AppVersionRepository
) : AppVersionPort {
    
    override fun getAppVersion(platform: String): AppVersion? {
        val platformEnum = try {
            Platform.valueOf(platform.uppercase())
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return repository.findByPlatform(platformEnum)?.toModel()
    }
    
    override fun saveAppVersion(appVersion: AppVersion): AppVersion {
        val platformEnum = Platform.valueOf(appVersion.platform.uppercase())
        
        // 기존 버전이 있으면 업데이트, 없으면 생성
        val existing = repository.findByPlatform(platformEnum)
        
        val entity = if (existing != null) {
            AppVersionEntity(
                id = existing.id,
                platform = platformEnum,
                minVersionCode = appVersion.minVersionCode,
                latestVersionCode = appVersion.latestVersionCode,
                forceUpdate = appVersion.forceUpdate,
                updateUrl = appVersion.updateUrl,
                message = appVersion.message,
                updatedAt = LocalDateTime.now()
            )
        } else {
            AppVersionEntity(
                platform = platformEnum,
                minVersionCode = appVersion.minVersionCode,
                latestVersionCode = appVersion.latestVersionCode,
                forceUpdate = appVersion.forceUpdate,
                updateUrl = appVersion.updateUrl,
                message = appVersion.message
            )
        }
        
        return repository.save(entity).toModel()
    }
    
    private fun AppVersionEntity.toModel() = AppVersion(
        platform = platform.name,
        minVersionCode = minVersionCode,
        latestVersionCode = latestVersionCode,
        forceUpdate = forceUpdate,
        updateUrl = updateUrl,
        message = message
    )
}
