plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:service"))
    implementation(project(":core:airkorea-adapter"))
    implementation(project(":core:openmeteo-adapter"))
    implementation(project(":core:persistence-adapter"))
    implementation(project(":core:fcm-adapter"))
    
    // Feature modules
    implementation(project(":feature:weather"))
    implementation(project(":feature:admin"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.seriouschoi.aircheck.AircheckServerApplicationKt")
}
