package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshToken
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.Hasher
import com.ohana.shared.exceptions.AuthorizationException
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberSignInHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var authMemberRepository: AuthMemberRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: MemberSignInHandler
    private lateinit var validator: Validator

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
        handler = MemberSignInHandler(unitOfWork)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should sign in member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                MemberSignInHandler.Request(
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = request.email,
                    password = Hasher.hashPassword(request.password, ByteArray(16)),
                    salt = ByteArray(16),
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { invocation ->
                val token = invocation.getArgument<RefreshToken>(0)
                token
            }

            val response = handler.handle(request)

            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(authMemberRepository).findByEmail(request.email)
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when member not found`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                MemberSignInHandler.Request(
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
                MemberSignInHandler.Request(
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
                MemberSignInHandler.Request(
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
    fun `handle should throw ValidationException when email is empty`() =
        runTest {
            val request =
                MemberSignInHandler.Request(
                    email = "",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("email" to "Email is required"))
        }

    @Test
    fun `handle should throw ValidationException when email format is invalid`() =
        runTest {
            val request =
                MemberSignInHandler.Request(
                    email = "invalid-email",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("email" to "Invalid email format"))
        }

    @Test
    fun `handle should throw ValidationException when password is empty`() =
        runTest {
            val request =
                MemberSignInHandler.Request(
                    email = "test@example.com",
                    password = "",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password is required"))
        }

    @Test
    fun `handle should accept valid email format`() =
        runTest {
            val request =
                MemberSignInHandler.Request(
                    email = "valid.email@example.com",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
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
                MemberSignInHandler.Request(
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
                MemberSignInHandler.Request(
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
