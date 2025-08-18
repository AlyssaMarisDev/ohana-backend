package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.Hasher
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

class LoginHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var authMemberRepository: AuthMemberRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: LoginHandler

    @BeforeEach
    fun setUp() {
        authMemberRepository = mock()
        refreshTokenRepository = mock()
        context =
            mock {
                on { authMembers } doReturn authMemberRepository
                on { refreshTokens } doReturn refreshTokenRepository
            }
        unitOfWork = mock()
        handler = LoginHandler(unitOfWork)
    }

    @Test
    fun `handle should return tokens when valid credentials provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val email = "test@example.com"
            val password = "password123"
            val salt = "salt".toByteArray()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val authMember =
                TestUtils.getAuthMember(
                    id = userId,
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = LoginHandler.Request(email, password)
            val response = handler.handle(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when email not found`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "nonexistent@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)

            val request = LoginHandler.Request(email, password)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid email or password", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should throw AuthorizationException when password is incorrect`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val email = "test@example.com"
            val correctPassword = "correctPassword"
            val wrongPassword = "wrongPassword"
            val salt = "salt".toByteArray()
            val hashedPassword = Hasher.hashPassword(correctPassword, salt)

            val authMember =
                TestUtils.getAuthMember(
                    id = userId,
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)

            val request = LoginHandler.Request(email, wrongPassword)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }

            assertEquals("Invalid email or password", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should create refresh token with correct properties`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val email = "test@example.com"
            val password = "password123"
            val salt = "salt".toByteArray()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val authMember =
                TestUtils.getAuthMember(
                    id = userId,
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = LoginHandler.Request(email, password)
            handler.handle(request)

            verify(refreshTokenRepository).create(
                argThat { refreshToken ->
                    refreshToken.userId == userId &&
                        refreshToken.token.isNotEmpty() &&
                        refreshToken.expiresAt.isAfter(Instant.now().plusSeconds(29 * 24 * 3600)) // 29 days
                },
            )
        }

    @Test
    fun `handle should work with empty password`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val email = "test@example.com"
            val password = ""
            val salt = "salt".toByteArray()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val authMember =
                TestUtils.getAuthMember(
                    id = userId,
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = LoginHandler.Request(email, password)
            val response = handler.handle(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should work with special characters in password`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val email = "test@example.com"
            val password = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
            val salt = "salt".toByteArray()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val authMember =
                TestUtils.getAuthMember(
                    id = userId,
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = LoginHandler.Request(email, password)
            val response = handler.handle(request)

            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenThrow(RuntimeException("DB error"))

            val request = LoginHandler.Request(email, password)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }

            assertEquals("DB error", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository, never()).create(any())
        }
}
