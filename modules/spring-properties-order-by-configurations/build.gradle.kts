plugins {
    id("framefork.java-public")
}

dependencies {
    api(libs.spring.boot.starter)
    api(libs.spring.boot.autoconfigure)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.description = "Reorders properties added via PropertySources into a Spring Boot application"
