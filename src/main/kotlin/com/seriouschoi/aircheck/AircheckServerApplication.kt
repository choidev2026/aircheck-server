package com.seriouschoi.aircheck

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableCaching
@EnableScheduling
class AircheckServerApplication

fun main(args: Array<String>) {
    runApplication<AircheckServerApplication>(*args)
}
