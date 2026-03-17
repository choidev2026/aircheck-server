plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // OpenAPI Schema annotations (#15)
    compileOnly("io.swagger.core.v3:swagger-annotations:2.2.19")
}
