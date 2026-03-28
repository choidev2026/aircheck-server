package com.seriouschoi.aircheck.core.persistence

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
import com.seriouschoi.aircheck.core.domain.port.ServiceVersionPort
import org.springframework.stereotype.Component

@Component
class ServiceVersionAdapter(
    private val repository: ServiceVersionRepository
) : ServiceVersionPort {
    
    override fun getServiceVersion(osType: String): ServiceVersion? {
        val osTypeEnum = try {
            OsType.valueOf(osType.uppercase())
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return repository.findByOsType(osTypeEnum)?.toModel()
    }
    
    private fun ServiceVersionEntity.toModel() = ServiceVersion(
        osType = osType.name,
        minVersionCode = minVersionCode,
        updateUrl = updateUrl
    )
}
