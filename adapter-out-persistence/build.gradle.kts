plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    
    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.3.3")
}
