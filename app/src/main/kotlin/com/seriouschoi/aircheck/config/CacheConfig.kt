package com.seriouschoi.aircheck.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        return object : CaffeineCacheManager() {
            override fun createNativeCaffeineCache(name: String): com.github.benmanes.caffeine.cache.Cache<Any, Any> {
                return when (name) {
                    // 측정소 정보: 24시간 (거의 안 바뀜)
                    "stations" -> Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(10)
                        .build()
                    
                    // 날씨/대기질: 1시간
                    else -> Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .build()
                }
            }
        }
    }
}
