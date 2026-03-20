package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.AppVersion
import com.seriouschoi.aircheck.core.domain.port.AppVersionPort
import org.springframework.stereotype.Service

@Service
class AppVersionService(
    private val appVersionPort: AppVersionPort
) {
    
    fun checkVersion(platform: String, currentVersionCode: Int): VersionCheckResult {
        val appVersion = appVersionPort.getAppVersion(platform)
            ?: return VersionCheckResult.notFound()
        
        val requiresUpdate = appVersion.requiresUpdate(currentVersionCode)
        val hasUpdate = appVersion.hasUpdate(currentVersionCode)
        
        return VersionCheckResult(
            minVersionCode = appVersion.minVersionCode,
            latestVersionCode = appVersion.latestVersionCode,
            latestVersionName = appVersion.latestVersionName,
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
    val minVersionCode: Int,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val forceUpdate: Boolean,
    val updateAvailable: Boolean,
    val updateUrl: String?,
    val message: String?
) {
    companion object {
        fun notFound() = VersionCheckResult(
            minVersionCode = 0,
            latestVersionCode = 0,
            latestVersionName = "0.0.0",
            forceUpdate = false,
            updateAvailable = false,
            updateUrl = null,
            message = null
        )
    }
}
