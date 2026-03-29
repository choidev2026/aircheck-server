package com.seriouschoi.aircheck.core.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ServiceConfigRepository : JpaRepository<ServiceConfigEntity, String> {
    fun findByConfigKey(configKey: String): ServiceConfigEntity?
}
