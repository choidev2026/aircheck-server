package com.seriouschoi.aircheck.core.domain.port

interface ServiceConfigPort {
    fun get(key: String): String?
    fun set(key: String, value: String)
}
