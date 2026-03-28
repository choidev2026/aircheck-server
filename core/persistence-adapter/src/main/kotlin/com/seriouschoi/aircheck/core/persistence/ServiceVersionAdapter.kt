package com.seriouschoi.aircheck.core.persistence

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
import com.seriouschoi.aircheck.core.domain.port.ServiceVersionPort
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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
    
    override fun saveServiceVersion(serviceVersion: ServiceVersion): ServiceVersion {
        val osTypeEnum = OsType.valueOf(serviceVersion.osType.uppercase())
        
        val entity = ServiceVersionEntity(
            osType = osTypeEnum,
            minVersionCode = serviceVersion.minVersionCode,
            updateUrl = serviceVersion.updateUrl,
            updatedAt = LocalDateTime.now()
        )
        
        return repository.save(entity).toModel()
    }
    
    private fun ServiceVersionEntity.toModel() = ServiceVersion(
        osType = osType.name,
        minVersionCode = minVersionCode,
        updateUrl = updateUrl
    )
}
