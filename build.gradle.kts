// This file is the master blueprint for the payment-engine build configuration.
// It manages our Java toolchain, project plugins, and external dependencies.

plugins {
    id("java")
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7" //Manage spring libraries compatibility
}

group = "com.payment"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 🗄️ Spring Data JPA (Provides Jakarta Persistence, Hibernate, and JpaRepository)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 🐘 PostgreSQL JDBC Driver
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}