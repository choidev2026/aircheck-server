package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.AppVersion

interface AppVersionPort {
    fun getAppVersion(platform: String): AppVersion?
    fun saveAppVersion(appVersion: AppVersion): AppVersion
}
