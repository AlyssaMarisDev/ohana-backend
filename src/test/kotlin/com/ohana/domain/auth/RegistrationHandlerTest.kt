package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.Hasher
import com.ohana.shared.exceptions.ConflictException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

class RegistrationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var authMemberRepository: AuthMemberRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var handler: RegistrationHandler

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
        handler = RegistrationHandler(unitOfWork)
    }

    @Test
    fun `handle should create member and return tokens when valid request provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            assertNotNull(response.id)
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(authMemberRepository).findByEmail(email)
            verify(authMemberRepository).create(
                argThat { member ->
                    member.name == name &&
                        member.email == email &&
                        member.password != password &&
                        // Should be hashed
                        member.salt.isNotEmpty()
                },
            )
            verify(refreshTokenRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository, refreshTokenRepository)
        }

    @Test
    fun `handle should throw ConflictException when email already exists`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "existing@example.com"
            val password = "password123"

            val existingMember = TestUtils.getAuthMember(email = email)
            whenever(authMemberRepository.findByEmail(email)).thenReturn(existingMember)

            val request = RegistrationHandler.Request(name, email, password)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(request)
                }

            assertEquals("Member with email $email already exists", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verify(authMemberRepository, never()).create(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should hash password correctly`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            handler.handle(request)

            verify(authMemberRepository).create(
                argThat { member ->
                    val hashedPassword = Hasher.hashPassword(password, member.salt)
                    member.password == hashedPassword
                },
            )
        }

    @Test
    fun `handle should create refresh token with correct properties`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            verify(refreshTokenRepository).create(
                argThat { refreshToken ->
                    refreshToken.userId == response.id &&
                        refreshToken.token.isNotEmpty() &&
                        refreshToken.expiresAt.isAfter(Instant.now().plusSeconds(29 * 24 * 3600)) // 29 days
                },
            )
        }

    @Test
    fun `handle should work with empty name`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = ""
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            assertNotNull(response.id)
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should work with empty password`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = ""

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            assertNotNull(response.id)
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should work with special characters in name and email`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User O'Connor-Smith"
            val email = "test.user+tag@example-domain.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            assertNotNull(response.id)
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should work with special characters in password`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            assertNotNull(response.id)
            assertNotNull(response.accessToken)
            assertNotNull(response.refreshToken)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenThrow(RuntimeException("DB error"))

            val request = RegistrationHandler.Request(name, email, password)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }

            assertEquals("DB error", ex.message)
            verify(authMemberRepository).findByEmail(email)
            verify(authMemberRepository, never()).create(any())
            verify(refreshTokenRepository, never()).create(any())
        }

    @Test
    fun `handle should generate unique IDs for member and refresh token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val name = "Test User"
            val email = "test@example.com"
            val password = "password123"

            whenever(authMemberRepository.findByEmail(email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { it.getArgument(0) }
            whenever(refreshTokenRepository.create(any())).thenAnswer { it.getArgument(0) }

            val request = RegistrationHandler.Request(name, email, password)
            val response = handler.handle(request)

            verify(authMemberRepository).create(
                argThat { member ->
                    member.id.isNotEmpty() && member.id != email
                },
            )
            verify(refreshTokenRepository).create(
                argThat { refreshToken ->
                    refreshToken.id.isNotEmpty() && refreshToken.id != response.id
                },
            )
        }
}
