package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.RefreshToken
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.JwtManager
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals

class LogoutHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: LogoutHandler

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock()
        context =
            mock {
                on { refreshTokens } doReturn refreshTokenRepository
            }
        unitOfWork = mock()
        handler = LogoutHandler(unitOfWork)
    }

    @Test
    fun `handle should logout successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = JwtManager.generateRefreshToken(userId)

            val storedToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = refreshToken,
                    userId = userId,
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
                    isRevoked = false,
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)

            val request = LogoutHandler.Request(refreshToken = refreshToken)
            val response = handler.handle(request)

            assertEquals("Successfully logged out", response.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository).revokeToken(refreshToken)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is invalid`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request = LogoutHandler.Request(refreshToken = "invalid-token")

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Invalid refresh token", ex.message)

            verifyNoInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token not found in database`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = JwtManager.generateRefreshToken(userId)

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(null)

            val request = LogoutHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Refresh token not found", ex.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user ID mismatch`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = JwtManager.generateRefreshToken(userId)

            val storedToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = refreshToken,
                    userId = UUID.randomUUID().toString(), // Different user ID
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
                    isRevoked = false,
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = LogoutHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Invalid refresh token", ex.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }
}
