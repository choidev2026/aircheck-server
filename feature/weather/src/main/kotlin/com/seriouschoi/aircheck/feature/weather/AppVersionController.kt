package com.seriouschoi.aircheck.feature.weather

import com.seriouschoi.aircheck.core.service.AppVersionService
import com.seriouschoi.aircheck.core.service.VersionCheckResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/app")
class AppVersionController(
    private val appVersionService: AppVersionService
) {
    
    /**
     * 앱 버전 체크 API
     * 
     * @param platform 플랫폼 (android, ios)
     * @param currentVersion 현재 앱 버전
     * @return 버전 체크 결과
     */
    @GetMapping("/version")
    fun checkVersion(
        @RequestParam platform: String,
        @RequestParam currentVersion: String
    ): ResponseEntity<VersionCheckResponse> {
        val result = appVersionService.checkVersion(platform, currentVersion)
        return ResponseEntity.ok(result.toResponse())
    }
}

data class VersionCheckResponse(
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val updateAvailable: Boolean,
    val updateUrl: String?,
    val message: String?
)

private fun VersionCheckResult.toResponse() = VersionCheckResponse(
    minVersion = minVersion,
    latestVersion = latestVersion,
    forceUpdate = forceUpdate,
    updateAvailable = updateAvailable,
    updateUrl = updateUrl,
    message = message
)
