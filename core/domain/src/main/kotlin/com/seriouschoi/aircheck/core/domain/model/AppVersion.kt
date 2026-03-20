package com.seriouschoi.aircheck.core.domain.model

data class AppVersion(
    val platform: String,
    val minVersionCode: Int,
    val latestVersionCode: Int,
    val forceUpdate: Boolean,
    val updateUrl: String?,
    val message: String?
) {
    fun requiresUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < minVersionCode
    }
    
    fun hasUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < latestVersionCode
    }
}
