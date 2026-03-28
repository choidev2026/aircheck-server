package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
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
    
    fun getServiceVersion(osType: String): ServiceVersion? {
        return serviceVersionPort.getServiceVersion(osType)
    }
    
    fun updateServiceVersion(osType: String, minVersionCode: Int, updateUrl: String?): ServiceVersion {
        val serviceVersion = ServiceVersion(
            osType = osType.uppercase(),
            minVersionCode = minVersionCode,
            updateUrl = updateUrl
        )
        return serviceVersionPort.saveServiceVersion(serviceVersion)
    }
}

data class VersionCheckResult(
    val needUpdate: Boolean,
    val updateUrl: String?
)
