plugins {
    // Provides Kotlin Language Support
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("jvm") version "2.2.0"

    // Provides the shadowJar task in Gradle
    // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("com.github.johnrengelman.shadow") version "8.1.1"

}

//Change this to reflect your package namespace
group = "com.nickcoblentz.montoya"
version = "0.1.0"

repositories {
    //add maven local in case you want to build some reusable libraries and host them within your home directory
    mavenLocal()

    mavenCentral()

    // Enable these if you want to use https://github.com/ncoblentz/BurpMontoyaLibrary
    // Add two specific GitHub repositories in which maven packages can be found through jitpack.io

    maven(url="https://jitpack.io") {
        content {
            includeGroup("com.github.ncoblentz")
        }
    }

}

dependencies {
    testImplementation(kotlin("test"))

    // Include the Montoya API from Maven Central:
    // https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api

    implementation("com.github.ncoblentz:BurpMontoyaLibrary:0.2.0")
    implementation("net.portswigger.burp.extensions:montoya-api:2025.6")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}