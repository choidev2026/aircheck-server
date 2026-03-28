package com.seriouschoi.aircheck.core.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ServiceVersionRepository : JpaRepository<ServiceVersionEntity, OsType> {
    fun findByOsType(osType: OsType): ServiceVersionEntity?
}
