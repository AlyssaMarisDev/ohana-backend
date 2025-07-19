package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.RefreshToken
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.JwtCreator
import com.ohana.shared.exceptions.AuthorizationException
import jakarta.validation.Validation
import jakarta.validation.Validator
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
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock()
        context =
            mock {
                on { refreshTokens } doReturn refreshTokenRepository
            }
        unitOfWork = mock()
        handler = LogoutHandler(unitOfWork)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should logout successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val refreshToken = JwtCreator.generateRefreshToken(userId)

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
            val refreshToken = JwtCreator.generateRefreshToken(userId)

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
            val refreshToken = JwtCreator.generateRefreshToken(userId)

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

    @Test
    fun `handle should throw ValidationException when refresh token is empty`() =
        runTest {
            val request = LogoutHandler.Request(refreshToken = "")
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assert(messages.contains("refreshToken" to "Refresh token is required"))
        }

    @Test
    fun `handle should throw ValidationException when refresh token is blank`() =
        runTest {
            val request = LogoutHandler.Request(refreshToken = "   ")
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assert(messages.contains("refreshToken" to "Refresh token is required"))
        }
}
