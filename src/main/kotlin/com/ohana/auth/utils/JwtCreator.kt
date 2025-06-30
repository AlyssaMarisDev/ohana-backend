package com.ohana.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class JwtCreator {
    companion object {
        private val JWT_SECRET = System.getenv("JWT_SECRET") ?: "a-string-secret-at-least-256-bits-long"

        fun generateToken(userId: Int): String =
            JWT
                .create()
                .withAudience("Ohana")
                .withIssuer("https://ohana.com")
                .withClaim("userId", userId)
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.DAYS))) // 30 days expiry
                .sign(Algorithm.HMAC256(JWT_SECRET))
    }
}
