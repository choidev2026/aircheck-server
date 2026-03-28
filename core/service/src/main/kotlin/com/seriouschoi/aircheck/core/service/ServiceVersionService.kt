package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.port.ServiceVersionPort
import org.springframework.stereotype.Service

@Service
class ServiceVersionService(
    private val serviceVersionPort: ServiceVersionPort
) {
    
    fun checkVersion(osType: String, currentVersionCode: Int): VersionCheckResult {
        val serviceVersion = serviceVersionPort.getServiceVersion(osType)
            ?: return VersionCheckResult(needUpdate = false, updateUrl = null)
        
        return VersionCheckResult(
            needUpdate = serviceVersion.needsUpdate(currentVersionCode),
            updateUrl = serviceVersion.updateUrl
        )
    }
}

data class VersionCheckResult(
    val needUpdate: Boolean,
    val updateUrl: String?
)
