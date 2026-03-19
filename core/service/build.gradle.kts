plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":core:domain"))
    
    // Spring (interface만 사용)
    implementation("org.springframework:spring-context:6.2.4")
    implementation("org.springframework:spring-tx:6.2.4")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    
    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
