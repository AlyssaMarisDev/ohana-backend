val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.0.0-rc-1"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "com.ohana"
version = "0.0.1"

application {
    mainClass.set("com.ohana.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")

    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:4.0.0") // Koin for Ktor integration
    implementation("io.insert-koin:koin-core:4.0.0") // Core Koin library

    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // JUnit 5 Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")

    // Database
    implementation("org.jdbi:jdbi3-core:3.27.0")
    implementation("org.jdbi:jdbi3-kotlin:3.27.0")
    implementation("org.jdbi:jdbi3-sqlobject:3.27.0")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:3.27.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    implementation("org.flywaydb:flyway-core:9.0.0")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
        showExceptions = true
        showStackTraces = true
        showCauses = true
    }
}

tasks.register("format") {
    dependsOn("ktlintFormat")
}

tasks.named("build") {
    dependsOn("ktlintFormat")
}

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
    coloredOutput.set(true)
    ignoreFailures.set(true)
}
