package com.ohana.domain.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class JwtCreator {
    companion object {
        private val JWT_SECRET = System.getenv("JWT_SECRET") ?: "a-string-secret-at-least-256-bits-long"
        private val JWT_EXPIRATION_HOURS = System.getenv("JWT_EXPIRATION_HOURS")?.toIntOrNull() ?: 1

        fun generateToken(userId: String): String =
            JWT
                .create()
                .withAudience("Ohana")
                .withIssuer("https://ohana.com")
                .withClaim("userId", userId)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(JWT_EXPIRATION_HOURS.toLong(), ChronoUnit.HOURS)))
                .sign(Algorithm.HMAC256(JWT_SECRET))
    }
}
