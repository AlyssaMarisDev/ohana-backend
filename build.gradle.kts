val kotlinVersion: String by project
val ktorVersion: String by project
val koinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
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
    // Web Server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Database
    implementation("org.jdbi:jdbi3-core:3.27.0")
    implementation("org.jdbi:jdbi3-kotlin:3.27.0")
    implementation("org.jdbi:jdbi3-sqlobject:3.27.0")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:3.27.0")
    implementation("com.mysql:mysql-connector-j:9.0.0")

    // Jackson JSON library
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testImplementation("mysql:mysql-connector-mxj:5.0.12")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
}

// Configure the JAR task to create an executable JAR
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.ohana.ApplicationKt",
        )
    }

    // Include all dependencies in the JAR (fat JAR)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        configurations.runtimeClasspath.map { config ->
            config.map { if (it.isDirectory) it else zipTree(it) }
        },
    )
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
