plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":adapter-in"))
    implementation(project(":adapter-out"))
    implementation(project(":adapter-out-weather"))
    implementation(project(":adapter-out-airquality"))
    implementation(project(":adapter-out-persistence"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // OpenAPI / Swagger UI (#15)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.seriouschoi.aircheck.AircheckServerApplicationKt")
}
