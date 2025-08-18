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

interface JwtService {
    fun validateRefreshToken(token: String): DecodedJWT?

    fun generateTokenPair(userId: String): TokenPair
}

class DefaultJwtService : JwtService {
    private val jwtConfig: JwtConfig = JwtConfig.fromEnvironment()

    override fun validateRefreshToken(token: String): DecodedJWT? =
        try {
            refreshVerifier().verify(token)
        } catch (e: JWTVerificationException) {
            null
        }

    override fun generateTokenPair(userId: String): TokenPair {
        val access = generateAccessToken(userId)
        Thread.sleep(1)
        val refresh = generateRefreshToken(userId)
        return TokenPair(access, refresh)
    }

    private fun generateAccessToken(userId: String): String =
        JWT
            .create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", userId)
            .withClaim("tokenType", "access")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plus(jwtConfig.expirationHours.toLong(), ChronoUnit.HOURS)))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

    private fun generateRefreshToken(userId: String): String =
        JWT
            .create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", userId)
            .withClaim("tokenType", "refresh")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plus(jwtConfig.refreshExpirationDays.toLong(), ChronoUnit.DAYS)))
            .sign(Algorithm.HMAC256(jwtConfig.refreshSecret))

    private fun accessVerifier(): JWTVerifier =
        JWT
            .require(Algorithm.HMAC256(jwtConfig.secret))
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("tokenType", "access")
            .build()

    private fun refreshVerifier(): JWTVerifier =
        JWT
            .require(Algorithm.HMAC256(jwtConfig.refreshSecret))
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("tokenType", "refresh")
            .build()
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
