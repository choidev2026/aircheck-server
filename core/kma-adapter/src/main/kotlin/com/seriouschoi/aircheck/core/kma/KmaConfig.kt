package com.seriouschoi.aircheck.core.kma

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaConfig
