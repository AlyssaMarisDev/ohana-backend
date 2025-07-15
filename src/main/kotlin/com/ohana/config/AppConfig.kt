package com.ohana.config

import io.ktor.server.config.*

data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val rateLimit: RateLimitConfig,
) {
    companion object {
        fun fromEnvironment(): AppConfig =
            AppConfig(
                server = ServerConfig.fromEnvironment(),
                database = DatabaseConfig.fromEnvironment(),
                jwt = JwtConfig.fromEnvironment(),
                rateLimit = RateLimitConfig.fromEnvironment(),
            )
    }
}

data class ServerConfig(
    val port: Int,
    val host: String,
    val environment: String,
) {
    companion object {
        fun fromEnvironment(): ServerConfig =
            ServerConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 4242,
                host = System.getenv("HOST") ?: "0.0.0.0",
                environment = System.getenv("ENVIRONMENT") ?: "development",
            )
    }
}

data class DatabaseConfig(
    val host: String,
    val port: String,
    val name: String,
    val user: String,
    val password: String,
    val connectionPoolSize: Int,
) {
    companion object {
        fun fromEnvironment(): DatabaseConfig =
            DatabaseConfig(
                host = System.getenv("DB_HOST") ?: "localhost",
                port = System.getenv("DB_PORT") ?: "3306",
                name = System.getenv("DB_NAME") ?: "ohana",
                user = System.getenv("DB_USER") ?: "root",
                password = System.getenv("DB_PASSWORD") ?: "root",
                connectionPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10,
            )
    }
}

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expirationHours: Long,
) {
    companion object {
        fun fromEnvironment(): JwtConfig =
            JwtConfig(
                secret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production",
                issuer = System.getenv("JWT_ISSUER") ?: "ohana-backend",
                audience = System.getenv("JWT_AUDIENCE") ?: "ohana-users",
                expirationHours = System.getenv("JWT_EXPIRATION_HOURS")?.toLongOrNull() ?: 24,
            )
    }
}

data class RateLimitConfig(
    val requestsPerMinute: Int,
    val burstCapacity: Int,
) {
    companion object {
        fun fromEnvironment(): RateLimitConfig =
            RateLimitConfig(
                requestsPerMinute = System.getenv("RATE_LIMIT_REQUESTS_PER_MINUTE")?.toIntOrNull() ?: 60,
                burstCapacity = System.getenv("RATE_LIMIT_BURST_CAPACITY")?.toIntOrNull() ?: 10,
            )
    }
}
