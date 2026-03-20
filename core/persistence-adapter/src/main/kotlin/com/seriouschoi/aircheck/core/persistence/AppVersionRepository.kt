package com.seriouschoi.aircheck.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppVersionRepository : JpaRepository<AppVersionEntity, Long> {
    fun findByPlatform(platform: Platform): AppVersionEntity?
}
