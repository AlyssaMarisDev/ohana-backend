package com.ohana.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory

fun Application.configureSecurity() {
    install(Authentication) {
        val jwtSecret = System.getenv("JWT_SECRET") ?: "a-string-secret-at-least-256-bits-long"
        val logger = LoggerFactory.getLogger("Security")

        jwt("auth-jwt") {
            realm = "Ohana"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience("Ohana")
                    .withIssuer("https://ohana.com")
                    .acceptExpiresAt(System.currentTimeMillis() / 1000) // Accept current time for expiration check
                    .build(),
            )
            validate { credential ->
                try {
                    if (credential.payload.audience.contains("Ohana")) {
                        // Check if token is expired
                        val expiresAt = credential.payload.expiresAt
                        if (expiresAt != null && expiresAt.before(java.util.Date())) {
                            logger.info("JWT token expired for user: ${credential.payload.getClaim("userId").asString()}")
                            null
                        } else {
                            JWTPrincipal(credential.payload)
                        }
                    } else {
                        null
                    }
                } catch (e: TokenExpiredException) {
                    logger.info("JWT token expired: ${e.message}")
                    null
                } catch (e: Exception) {
                    logger.error("JWT validation error: ${e.message}")
                    null
                }
            }
        }
    }
}
