package com.seriouschoi.aircheck.core.persistence

import com.seriouschoi.aircheck.core.domain.port.ServiceConfigPort
import org.springframework.stereotype.Component

@Component
class ServiceConfigAdapter(
    private val repository: ServiceConfigRepository
) : ServiceConfigPort {
    
    override fun get(key: String): String? {
        return repository.findByConfigKey(key)?.configValue
    }
    
    override fun set(key: String, value: String) {
        val entity = repository.findByConfigKey(key)
        if (entity != null) {
            entity.update(value)
            repository.save(entity)
        } else {
            repository.save(ServiceConfigEntity(configKey = key, configValue = value))
        }
    }
}
