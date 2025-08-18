package com.ohana.domain.auth.utils

import com.auth0.jwt.JWT
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class JwtServiceTest {
    private val jwtService: JwtService = DefaultJwtService()

    @Test
    fun `generateTokenPair should produce non-empty distinct tokens and valid refresh`() =
        runTest {
            val userId = UUID.randomUUID().toString()

            val pair = jwtService.generateTokenPair(userId)

            assertTrue(pair.accessToken.isNotEmpty())
            assertTrue(pair.refreshToken.isNotEmpty())
            assertTrue(pair.accessToken != pair.refreshToken)

            val decoded = jwtService.validateRefreshToken(pair.refreshToken)
            assertNotNull(decoded)
            assertEquals("refresh", decoded!!.getClaim("tokenType").asString())
            assertEquals(userId, decoded.getClaim("userId").asString())

            // Ensure refresh expiry roughly >= 29 days from now (config default is 30)
            val expiresAt = decoded.expiresAt.toInstant()
            assertTrue(expiresAt.isAfter(Instant.now().plus(29, ChronoUnit.DAYS)))
        }

    @Test
    fun `validateRefreshToken should return null for invalid or access token`() =
        runTest {
            assertNull(jwtService.validateRefreshToken("invalid-token"))

            val userId = UUID.randomUUID().toString()
            val pair = jwtService.generateTokenPair(userId)

            // Access token is not a refresh token
            assertNull(jwtService.validateRefreshToken(pair.accessToken))
        }

    @Test
    fun `generateTokenPair should produce valid tokens on repeated calls`() =
        runTest {
            val userId = UUID.randomUUID().toString()
            val p1 = jwtService.generateTokenPair(userId)
            val p2 = jwtService.generateTokenPair(userId)

            assertTrue(p1.refreshToken.isNotEmpty())
            assertTrue(p2.refreshToken.isNotEmpty())

            val r1 = jwtService.validateRefreshToken(p1.refreshToken)
            val r2 = jwtService.validateRefreshToken(p2.refreshToken)
            assertNotNull(r1)
            assertNotNull(r2)
            assertEquals(userId, r1!!.getClaim("userId").asString())
            assertEquals(userId, r2!!.getClaim("userId").asString())

            val accessDecoded = JWT.decode(p1.accessToken)
            assertEquals("access", accessDecoded.getClaim("tokenType").asString())
            assertEquals(userId, accessDecoded.getClaim("userId").asString())
        }
}
