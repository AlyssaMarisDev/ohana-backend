package com.ohana

import com.ohana.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin

fun main() {
    val config = AppConfig.fromEnvironment()
    // Run Flyway migrations before starting the server
    runDatabaseMigrations(config.database)

    embeddedServer(
        Netty,
        port = config.server.port,
        host = config.server.host,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    // Ensure migrations also run in test hosts that call module() directly
    runDatabaseMigrations(config.database)

    install(Koin) {
        modules(appModule)
    }

    configureSecurity(config.jwt)
    configureSerialization()
    configureCORS()
    configureRateLimit(config.rateLimit)
    configureRouting()
    configureCallLogging()
    configureExceptionHandling()
}
