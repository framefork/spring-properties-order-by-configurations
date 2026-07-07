pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.framefork.build") version "0.2.0"
}

framefork {
    minJavaVersion = 17
    jdkVersion = 21
}

rootProject.name = "spring-properties-order-by-configurations"
