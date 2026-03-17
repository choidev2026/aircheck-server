plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":domain"))
    
    // Spring Web (Controller)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // OpenAPI / Swagger (#15)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}
