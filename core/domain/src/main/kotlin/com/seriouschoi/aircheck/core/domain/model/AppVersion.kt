package com.seriouschoi.aircheck.core.domain.model

data class AppVersion(
    val platform: String,
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val updateUrl: String?,
    val message: String?
) {
    /**
     * 현재 버전이 최소 버전보다 낮은지 확인
     */
    fun requiresUpdate(currentVersion: String): Boolean {
        return compareVersions(currentVersion, minVersion) < 0
    }
    
    /**
     * 현재 버전이 최신 버전보다 낮은지 확인
     */
    fun hasUpdate(currentVersion: String): Boolean {
        return compareVersions(currentVersion, latestVersion) < 0
    }
    
    companion object {
        /**
         * 버전 비교 (semver 형식: x.y.z)
         * @return 음수: v1 < v2, 0: v1 == v2, 양수: v1 > v2
         */
        fun compareVersions(v1: String, v2: String): Int {
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(parts1.size, parts2.size)
            
            for (i in 0 until maxLength) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) return p1 - p2
            }
            return 0
        }
    }
}
