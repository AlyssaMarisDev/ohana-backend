package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.JwtService
import com.ohana.domain.auth.utils.TokenPair
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TokenRefreshHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: TokenRefreshHandler
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
        handler = TokenRefreshHandler(unitOfWork, jwtService)
    }

    @Test
    fun `handle should return new token pair when valid refresh token provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                    expiresAt = Instant.now().plusSeconds(3600), // Not expired
                )

            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)
            whenever(jwtService.generateTokenPair(userId)).thenReturn(TokenPair("access", "new-refresh"))
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = TokenRefreshHandler.Request(refreshToken)
            val response = handler.handle(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository).revokeToken(refreshToken)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is invalid`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = "invalid-refresh-token"
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(null)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository, never()).findByToken(any())
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token has no userId claim`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = null)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository, never()).findByToken(any())
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token not found in database`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = UUID.randomUUID().toString())
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)
            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(null)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Refresh token not found", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is revoked`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = true, // Revoked
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Refresh token has been revoked", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when refresh token is expired`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                    expiresAt = Instant.now().minusSeconds(3600), // Expired
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Refresh token has expired", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when stored token userId doesn't match decoded token userId`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val storedUserId = UUID.randomUUID().toString() // Different user
            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = UUID.randomUUID().toString())
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = storedUserId,
                    isRevoked = false,
                    expiresAt = Instant.now().plusSeconds(3600),
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid refresh token", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository, never()).revokeToken(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should create new refresh token with correct properties`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)
            whenever(jwtService.generateTokenPair(userId)).thenReturn(TokenPair("access", "new-refresh"))

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                    expiresAt = Instant.now().plusSeconds(3600),
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = TokenRefreshHandler.Request(refreshToken)
            handler.handle(request)

            verify(refreshTokenRepository).create(
                argThat { newRefreshToken ->
                    newRefreshToken.userId == userId &&
                        newRefreshToken.token.isNotEmpty() &&
                        newRefreshToken.token != refreshToken &&
                        // Should be different token
                        newRefreshToken.expiresAt.isAfter(Instant.now().plusSeconds(29 * 24 * 3600)) // 29 days
                },
            )
        }

    @Test
    fun `handle should work with empty refresh token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val refreshToken = ""

            val request = TokenRefreshHandler.Request(refreshToken)

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
            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                    expiresAt = Instant.now().plusSeconds(3600),
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenThrow(RuntimeException("DB error"))

            val request = TokenRefreshHandler.Request(refreshToken)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }

            assertEquals("DB error", ex.message)
            verify(refreshTokenRepository).findByToken(refreshToken)
            verify(refreshTokenRepository).revokeToken(refreshToken)
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should generate unique IDs for new refresh token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = "valid-refresh-token"
            val decoded = TestUtils.mockDecodedJwt(userId = userId)
            whenever(jwtService.validateRefreshToken(refreshToken)).thenReturn(decoded)

            val storedToken =
                TestUtils.getRefreshToken(
                    token = refreshToken,
                    userId = userId,
                    isRevoked = false,
                    expiresAt = Instant.now().plusSeconds(3600),
                )

            whenever(refreshTokenRepository.findByToken(refreshToken)).thenReturn(storedToken)
            whenever(refreshTokenRepository.revokeToken(refreshToken)).thenReturn(true)
            whenever(jwtService.generateTokenPair(userId)).thenReturn(TokenPair("access", "new-refresh"))
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = TokenRefreshHandler.Request(refreshToken)
            handler.handle(request)

            verify(refreshTokenRepository).create(
                argThat { newRefreshToken ->
                    newRefreshToken.id.isNotEmpty() && newRefreshToken.id != userId
                },
            )
        }
}
