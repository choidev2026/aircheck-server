package com.seriouschoi.aircheck.core.domain.model

data class AppVersion(
    val platform: String,
    val minVersionCode: Int,        // 최소 지원 버전 코드
    val latestVersionCode: Int,     // 최신 버전 코드
    val latestVersionName: String,  // 최신 버전명 (표시용)
    val forceUpdate: Boolean,
    val updateUrl: String?,
    val message: String?
) {
    /**
     * 현재 버전이 최소 버전보다 낮은지 확인
     */
    fun requiresUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < minVersionCode
    }
    
    /**
     * 현재 버전이 최신 버전보다 낮은지 확인
     */
    fun hasUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < latestVersionCode
    }
}
