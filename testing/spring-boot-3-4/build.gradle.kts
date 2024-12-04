plugins {
    id("framefork.java")
}

dependencies {
    implementation(project(":spring-properties-order-by-configurations"))

    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    implementation(libs.logback.classic)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
