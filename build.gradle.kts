val kotlinVersion: String by project
val ktorVersion: String by project
val koinVersion: String by project
val logbackVersion: String by project
val ktlintPluginVersion: String by project
val versionsPluginVersion: String by project
val jdbiVersion: String by project
val mysqlConnectorVersion: String by project
val jacksonVersion: String by project
val junitVersion: String by project
val h2Version: String by project
val coroutinesTestVersion: String by project
val mockitoCoreVersion: String by project
val mockitoKotlinVersion: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.ben-manes.versions")
}

kotlin {
    jvmToolchain(17)
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
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Database
    implementation("org.jdbi:jdbi3-core:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin:$jdbiVersion")
    implementation("org.jdbi:jdbi3-sqlobject:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:$jdbiVersion")
    implementation("com.mysql:mysql-connector-j:$mysqlConnectorVersion")

    // Jackson JSON library
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("mysql:mysql-connector-mxj:5.0.12")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    testImplementation("org.mockito:mockito-core:$mockitoCoreVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
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

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
    coloredOutput.set(true)
    ignoreFailures.set(true)
}
