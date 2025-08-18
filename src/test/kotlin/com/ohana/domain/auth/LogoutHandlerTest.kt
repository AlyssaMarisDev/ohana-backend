package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.JwtService
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class LogoutHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: LogoutHandler
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock()
        context =
            mock {
                on { refreshTokens } doReturn refreshTokenRepository
            }
        unitOfWork = mock()
        jwtService = mock()
        handler = LogoutHandler(unitOfWork, jwtService)
    }

    @Test
    fun `handle should revoke token and return success message when valid refresh token provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(userId)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                )

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)

            val request = LogoutHandler.Request(refreshToken)
            val response = handler.handle(request)

            assertEquals("Successfully logged out", response.message)

            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository).revokeToken(refreshToken)
            verifyNoMoreInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is invalid`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = "invalid-refresh-token"

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(null)

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository, never()).findByToken(any())
            verify(refreshTokenRepository, never()).revokeToken(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token has no userId claim`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(null)

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository, never()).findByToken(any())
            verify(refreshTokenRepository, never()).revokeToken(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token not found in database`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(userId)

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(null)

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Refresh token not found", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
        }

    @Test
    fun `handle should throw AuthorizationException when stored token userId doesn't match decoded token userId`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val decodedUserId = UUID.randomUUID().toString()
            val storedUserId = UUID.randomUUID().toString() // Different user
            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(decodedUserId)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = storedUserId,
                    isRevoked = false,
                )

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
        }

    @Test
    fun `handle should work with already revoked token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(userId)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = true, // Already revoked
                )

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)

            val request = LogoutHandler.Request(refreshToken)
            val response = handler.handle(request)

            assertEquals("Successfully logged out", response.message)
            verify(refreshTokenRepository).revokeToken(refreshToken)
        }

    @Test
    fun `handle should work with empty refresh token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = ""

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(null)

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decodedToken = TestUtils.mockDecodedJwt(userId)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                )

            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decodedToken)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenThrow(RuntimeException("DB error"))

            val request = LogoutHandler.Request(refreshToken)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }

            assertEquals("DB error", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository).revokeToken(refreshToken)
        }
}
