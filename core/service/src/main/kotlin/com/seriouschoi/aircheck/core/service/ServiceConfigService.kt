package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.port.ServiceConfigPort
import org.springframework.stereotype.Service

@Service
class ServiceConfigService(
    private val configPort: ServiceConfigPort
) {
    companion object {
        const val KEY_APPCHECK_ENABLED = "appcheck_enabled"
        const val KEY_KMA_PARALLEL_ENABLED = "kma_parallel_enabled"
    }
    
    fun get(key: String): String? {
        return configPort.get(key)
    }
    
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return get(key)?.toBooleanStrictOrNull() ?: default
    }
    
    fun set(key: String, value: String) {
        configPort.set(key, value)
    }
    
    fun isAppCheckEnabled(): Boolean {
        return getBoolean(KEY_APPCHECK_ENABLED, default = true)
    }
    
    fun setAppCheckEnabled(enabled: Boolean) {
        set(KEY_APPCHECK_ENABLED, enabled.toString())
    }
    
    fun isKmaParallelEnabled(): Boolean {
        return getBoolean(KEY_KMA_PARALLEL_ENABLED, default = true)
    }
    
    fun setKmaParallelEnabled(enabled: Boolean) {
        set(KEY_KMA_PARALLEL_ENABLED, enabled.toString())
    }
}
