package com.ohana.domain.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.ohana.plugins.JwtConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class JwtManager {
    companion object {
        private val jwtConfig = JwtConfig.fromEnvironment()

        fun generateAccessToken(userId: String): String =
            JWT
                .create()
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .withClaim("userId", userId)
                .withClaim("tokenType", "access")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(jwtConfig.expirationHours.toLong(), ChronoUnit.HOURS)))
                .sign(Algorithm.HMAC256(jwtConfig.secret))

        fun generateRefreshToken(userId: String): String =
            JWT
                .create()
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .withClaim("userId", userId)
                .withClaim("tokenType", "refresh")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(jwtConfig.refreshExpirationDays.toLong(), ChronoUnit.DAYS)))
                .sign(Algorithm.HMAC256(jwtConfig.refreshSecret))

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
                        .require(Algorithm.HMAC256(jwtConfig.secret))
                        .withAudience(jwtConfig.audience)
                        .withIssuer(jwtConfig.issuer)
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
                        .require(Algorithm.HMAC256(jwtConfig.refreshSecret))
                        .withAudience(jwtConfig.audience)
                        .withIssuer(jwtConfig.issuer)
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
