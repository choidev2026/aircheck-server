plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    kotlin("plugin.jpa")
}

dependencies {
    // adapter는 domain만 의존 (Port 인터페이스만 앎)
    // 실제 구현체는 app 모듈에서 주입됨
    implementation(project(":domain"))
    
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    
    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Firebase Admin (FCM)
    implementation("com.google.firebase:firebase-admin:9.3.0")
}
