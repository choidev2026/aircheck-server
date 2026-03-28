package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion

interface ServiceVersionPort {
    fun getServiceVersion(osType: String): ServiceVersion?
}
