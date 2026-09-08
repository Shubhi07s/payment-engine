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

    // Spring Data JPA (Provides Jakarta Persistence, Hibernate, and JpaRepository)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation ("org.springframework.kafka:spring-kafka")

    implementation ("org.springframework.boot:spring-boot-starter-data-redis")

    // PostgreSQL JDBC Driver
    runtimeOnly("org.postgresql:postgresql")

    // Spring Boot Test Starter (Includes Mockito, MockMvc, @WebMvcTest, and JUnit 5)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}