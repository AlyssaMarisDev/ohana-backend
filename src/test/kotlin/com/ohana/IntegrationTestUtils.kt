package com.ohana

import com.ohana.plugins.*
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

/**
 * Utility functions for integration testing
 */
object IntegrationTestUtils {
    /**
     * Configures a test application with minimal plugins for simple endpoints
     * Use this when you only need basic functionality (e.g., health checks)
     */
    fun Application.configureMinimalTestApplication() {
        val config = AppConfig.fromEnvironment()

        install(Koin) {
            modules(appModule)
        }

        // Install only essential plugins
        configureSecurity(config.jwt)
        configureSerialization()
        configureRouting()
        configureCallLogging()
        configureExceptionHandling()
    }

    /**
     * Configures a test application with database support
     * Use this when testing endpoints that require database operations
     */
    fun Application.configureTestApplicationWithDatabase() {
        val config = AppConfig.fromEnvironment()

        install(Koin) {
            modules(appModule)
        }

        // Install all plugins including database-related ones
        configureSecurity(config.jwt)
        configureSerialization()
        configureCORS()
        configureRateLimit(config.rateLimit)
        configureValidation()
        configureRouting()
        configureCallLogging()
        configureExceptionHandling()
    }
}
