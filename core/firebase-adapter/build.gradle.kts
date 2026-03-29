plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Internal modules
    implementation(project(":core:domain"))
    
    // Spring
    implementation("org.springframework:spring-context:6.2.4")
    implementation("org.springframework:spring-web:6.2.4")
    
    // Jakarta
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    
    // Firebase Admin (App Check 검증용)
    implementation("com.google.firebase:firebase-admin:9.3.0")
    
    // JWT 검증
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.auth0:jwks-rsa:0.22.1")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
