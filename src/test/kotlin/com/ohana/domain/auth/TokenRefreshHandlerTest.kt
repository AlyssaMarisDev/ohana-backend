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
import kotlin.test.assertNotNull

class TokenRefreshHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: TokenRefreshHandler

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock()
        context =
            mock {
                on { refreshTokens } doReturn refreshTokenRepository
            }
        unitOfWork = mock()
        handler = TokenRefreshHandler(unitOfWork)
    }

    @Test
    fun `handle should refresh tokens successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val oldRefreshToken = JwtManager.generateRefreshToken(userId)

            val storedToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = oldRefreshToken,
                    userId = userId,
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
                    isRevoked = false,
                )

            whenever(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(oldRefreshToken)).thenReturn(true)
            whenever(refreshTokenRepository.create(any())).thenAnswer { invocation ->
                val token = invocation.getArgument<RefreshToken>(0)
                token
            }

            val request = TokenRefreshHandler.Request(refreshToken = oldRefreshToken)
            val response = handler.handle(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            // The new tokens should be different from each other
            assert(response.accessToken != response.refreshToken)

            verify(refreshTokenRepository).findByToken(oldRefreshToken)
            verify(refreshTokenRepository).revokeToken(oldRefreshToken)
            verify(refreshTokenRepository).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is invalid`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request = TokenRefreshHandler.Request(refreshToken = "invalid-token")

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

            val request = TokenRefreshHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Refresh token not found", ex.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is revoked`() =
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
                    isRevoked = true,
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = TokenRefreshHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Refresh token has been revoked", ex.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is expired`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = JwtManager.generateRefreshToken(userId)

            val storedToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = refreshToken,
                    userId = userId,
                    expiresAt = Instant.now().minus(1, ChronoUnit.DAYS), // Expired
                    isRevoked = false,
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = TokenRefreshHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Refresh token has expired", ex.message)

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

            val request = TokenRefreshHandler.Request(refreshToken = refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Invalid refresh token", ex.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }
}
