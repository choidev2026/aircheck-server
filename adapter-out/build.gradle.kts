plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":domain"))
    
    // Spring
    implementation("org.springframework:spring-context:6.2.4")
    
    // Jakarta (for @PostConstruct)
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    
    // Firebase Admin (FCM)
    implementation("com.google.firebase:firebase-admin:9.3.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
