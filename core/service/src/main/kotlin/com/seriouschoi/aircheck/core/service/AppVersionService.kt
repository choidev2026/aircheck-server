package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.AppVersion
import com.seriouschoi.aircheck.core.domain.port.AppVersionPort
import org.springframework.stereotype.Service

@Service
class AppVersionService(
    private val appVersionPort: AppVersionPort
) {
    
    fun checkVersion(platform: String, currentVersion: String): VersionCheckResult {
        val appVersion = appVersionPort.getAppVersion(platform)
            ?: return VersionCheckResult.notFound()
        
        val requiresUpdate = appVersion.requiresUpdate(currentVersion)
        val hasUpdate = appVersion.hasUpdate(currentVersion)
        
        return VersionCheckResult(
            minVersion = appVersion.minVersion,
            latestVersion = appVersion.latestVersion,
            forceUpdate = requiresUpdate && appVersion.forceUpdate,
            updateAvailable = hasUpdate,
            updateUrl = appVersion.updateUrl,
            message = if (requiresUpdate) appVersion.message else null
        )
    }
    
    fun updateVersion(appVersion: AppVersion): AppVersion {
        return appVersionPort.saveAppVersion(appVersion)
    }
}

data class VersionCheckResult(
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val updateAvailable: Boolean,
    val updateUrl: String?,
    val message: String?
) {
    companion object {
        fun notFound() = VersionCheckResult(
            minVersion = "0.0.0",
            latestVersion = "0.0.0",
            forceUpdate = false,
            updateAvailable = false,
            updateUrl = null,
            message = null
        )
    }
}
