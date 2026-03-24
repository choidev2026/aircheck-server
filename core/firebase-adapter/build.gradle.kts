plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Spring
    implementation("org.springframework:spring-context:6.2.4")
    implementation("org.springframework:spring-web:6.2.4")
    
    // Jakarta Servlet
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    
    // Firebase Admin (App Check 검증용)
    implementation("com.google.firebase:firebase-admin:9.3.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
