plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":domain"))
    
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    
    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Firebase Admin (FCM)
    implementation("com.google.firebase:firebase-admin:9.3.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
