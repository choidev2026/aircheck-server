package com.seriouschoi.aircheck.core.kma

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaConfig {
    
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
