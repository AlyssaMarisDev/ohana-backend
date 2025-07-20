package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.Hasher
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `handle should sign in member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "test@example.com"
            val password = "ValidPass123!"
            val salt = Hasher.generateSalt()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val request =
                LoginHandler.Request(
                    email = email,
                    password = password,
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)

            val response = handler.handle(request)

            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when member not found`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                LoginHandler.Request(
                    email = "nonexistent@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Invalid email or password", ex.message)
            verify(authMemberRepository).findByEmail(request.email)
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when password is incorrect`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "test@example.com"
            val correctPassword = "ValidPass123!"
            val wrongPassword = "WrongPass123!"
            val salt = Hasher.generateSalt()
            val hashedPassword = Hasher.hashPassword(correctPassword, salt)

            val request =
                LoginHandler.Request(
                    email = email,
                    password = wrongPassword,
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(request)
                }
            assertEquals("Invalid email or password", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                LoginHandler.Request(
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }
            assertEquals("DB error", ex.message)
            verify(authMemberRepository).findByEmail(request.email)
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should generate JWT token for successful sign in`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "test@example.com"
            val password = "ValidPass123!"
            val salt = Hasher.generateSalt()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val request =
                LoginHandler.Request(
                    email = email,
                    password = password,
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)

            val response = handler.handle(request)

            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())
            assertTrue(response.accessToken != authMember.id) // Token should be different from ID
            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should verify password hash correctly`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val email = "test@example.com"
            val password = "ValidPass123!"
            val salt = Hasher.generateSalt()
            val hashedPassword = Hasher.hashPassword(password, salt)

            val request =
                LoginHandler.Request(
                    email = email,
                    password = password,
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = email,
                    password = hashedPassword,
                    salt = salt,
                )

            whenever(authMemberRepository.findByEmail(email)).thenReturn(authMember)

            val response = handler.handle(request)

            // Verify that the password hash verification works correctly
            assertTrue(Hasher.hashPassword(password, salt) == hashedPassword)
            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())
            verify(authMemberRepository).findByEmail(email)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }
}
