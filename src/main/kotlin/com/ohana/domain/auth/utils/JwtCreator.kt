package com.ohana.domain.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class JwtCreator {
    companion object {
        private val JWT_SECRET = System.getenv("JWT_SECRET") ?: "a-string-secret-at-least-256-bits-long"
        private val JWT_REFRESH_SECRET = System.getenv("JWT_REFRESH_SECRET") ?: "a-refresh-secret-at-least-256-bits-long"
        private val JWT_EXPIRATION_HOURS = System.getenv("JWT_EXPIRATION_HOURS")?.toIntOrNull() ?: 1
        private val JWT_REFRESH_EXPIRATION_DAYS = System.getenv("JWT_REFRESH_EXPIRATION_DAYS")?.toIntOrNull() ?: 30
        private val JWT_ISSUER = System.getenv("JWT_ISSUER") ?: "ohana-backend"
        private val JWT_AUDIENCE = System.getenv("JWT_AUDIENCE") ?: "ohana-users"

        fun generateAccessToken(userId: String): String =
            JWT
                .create()
                .withAudience(JWT_AUDIENCE)
                .withIssuer(JWT_ISSUER)
                .withClaim("userId", userId)
                .withClaim("tokenType", "access")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(JWT_EXPIRATION_HOURS.toLong(), ChronoUnit.HOURS)))
                .sign(Algorithm.HMAC256(JWT_SECRET))

        fun generateRefreshToken(userId: String): String =
            JWT
                .create()
                .withAudience(JWT_AUDIENCE)
                .withIssuer(JWT_ISSUER)
                .withClaim("userId", userId)
                .withClaim("tokenType", "refresh")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(JWT_REFRESH_EXPIRATION_DAYS.toLong(), ChronoUnit.DAYS)))
                .sign(Algorithm.HMAC256(JWT_REFRESH_SECRET))

        fun generateTokenPair(userId: String): TokenPair {
            val accessToken = generateAccessToken(userId)
            // Add a small delay to ensure different timestamps
            Thread.sleep(1)
            val refreshToken = generateRefreshToken(userId)
            return TokenPair(accessToken, refreshToken)
        }

        fun validateAccessToken(token: String): DecodedJWT? =
            try {
                val verifier: JWTVerifier =
                    JWT
                        .require(Algorithm.HMAC256(JWT_SECRET))
                        .withAudience(JWT_AUDIENCE)
                        .withIssuer(JWT_ISSUER)
                        .withClaim("tokenType", "access")
                        .build()
                verifier.verify(token)
            } catch (e: JWTVerificationException) {
                null
            }

        fun validateRefreshToken(token: String): DecodedJWT? =
            try {
                val verifier: JWTVerifier =
                    JWT
                        .require(Algorithm.HMAC256(JWT_REFRESH_SECRET))
                        .withAudience(JWT_AUDIENCE)
                        .withIssuer(JWT_ISSUER)
                        .withClaim("tokenType", "refresh")
                        .build()
                verifier.verify(token)
            } catch (e: JWTVerificationException) {
                null
            }

        fun getUserIdFromToken(token: String): String? = validateAccessToken(token)?.getClaim("userId")?.asString()

        fun getUserIdFromRefreshToken(token: String): String? = validateRefreshToken(token)?.getClaim("userId")?.asString()
    }
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
