plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":domain"))
    
    // Spring (interface만 사용)
    implementation("org.springframework:spring-context:6.2.4")
    implementation("org.springframework:spring-tx:6.2.4")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
}
