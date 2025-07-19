package com.ohana.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    install(Authentication) {
        val logger = LoggerFactory.getLogger("Security")

        jwt("auth-jwt") {
            realm = "Ohana"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .withClaim("tokenType", "access")
                    .build(),
            )
            validate { credential ->
                try {
                    if (credential.payload.audience.contains(jwtConfig.audience)) {
                        JWTPrincipal(credential.payload)
                    } else {
                        logger.info("JWT token audience mismatch")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("JWT validation error: ${e.message}")
                    null
                }
            }
        }
    }
}
