package com.seriouschoi.aircheck.feature.weather

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Admin 관리 API
 */
@RestController
@RequestMapping("/api/admin")
class AdminController(
    @Value("\${admin.api-key:}") private val adminApiKey: String
) {
    // 인메모리 저장 (실제로는 DB 사용 권장)
    private val versionStore = ConcurrentHashMap<String, Any>().apply {
        put("latestVersion", "1.0.0")
        put("minimumVersion", "1.0.0")
        put("forceUpdate", false)
        put("updateUrl", "https://play.google.com/store/apps/details?id=seriouschoi.aircheck")
    }

    /**
     * 버전 정보 조회
     */
    @GetMapping("/version")
    fun getVersion(
        @RequestHeader("X-Admin-Api-Key") apiKey: String
    ): ResponseEntity<Map<String, Any>> {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok(versionStore.toMap())
    }

    /**
     * 버전 정보 수정
     */
    @PutMapping("/version")
    fun updateVersion(
        @RequestHeader("X-Admin-Api-Key") apiKey: String,
        @RequestBody request: VersionUpdateRequest
    ): ResponseEntity<Map<String, Any>> {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(401).build()
        }
        
        versionStore["latestVersion"] = request.latestVersion
        versionStore["minimumVersion"] = request.minimumVersion
        versionStore["forceUpdate"] = request.forceUpdate
        versionStore["updateUrl"] = request.updateUrl
        
        return ResponseEntity.ok(versionStore.toMap())
    }

    private fun isValidApiKey(apiKey: String): Boolean {
        return adminApiKey.isNotBlank() && apiKey == adminApiKey
    }
}

data class VersionUpdateRequest(
    val latestVersion: String,
    val minimumVersion: String,
    val forceUpdate: Boolean,
    val updateUrl: String
)
