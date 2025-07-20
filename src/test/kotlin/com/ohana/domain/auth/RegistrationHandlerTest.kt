package com.ohana.domain.auth

import com.ohana.TestUtils
import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshToken
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.Hasher
import com.ohana.shared.exceptions.ConflictException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `handle should register member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            val authMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = request.name,
                    email = request.email,
                    password = "hashedPassword",
                    salt = ByteArray(16),
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenReturn(authMember)
            whenever(refreshTokenRepository.create(any())).thenAnswer { invocation ->
                val token = invocation.getArgument<RefreshToken>(0)
                token
            }

            val response = handler.handle(request)

            assertEquals(authMember.id, response.id)
            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())

            verify(authMemberRepository).findByEmail(request.email)
            verify(authMemberRepository).create(
                argThat { member ->
                    member.name == request.name &&
                        member.email == request.email &&
                        member.password != request.password &&
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

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "existing@example.com",
                    password = "ValidPass123!",
                )

            val existingMember =
                AuthMember(
                    id = UUID.randomUUID().toString(),
                    name = "Existing User",
                    email = request.email,
                    password = "hashedPassword",
                    salt = ByteArray(16),
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(existingMember)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(request)
                }
            assertEquals("Member with email ${request.email} already exists", ex.message)
            verify(authMemberRepository).findByEmail(request.email)
            verifyNoMoreInteractions(authMemberRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(request)
                }
            assertEquals("DB error", ex.message)
            verify(authMemberRepository).findByEmail(request.email)
            verify(authMemberRepository).create(any())
            verifyNoMoreInteractions(authMemberRepository)
        }

    @Test
    fun `handle should hash password correctly`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { invocation ->
                val member = invocation.getArgument<AuthMember>(0)
                member
            }

            handler.handle(request)

            verify(authMemberRepository).create(
                argThat { member ->
                    member.password != request.password &&
                        // Should be hashed
                        member.salt.isNotEmpty() &&
                        Hasher.hashPassword(request.password, member.salt) == member.password
                },
            )
        }

    @Test
    fun `handle should generate unique ID for member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { invocation ->
                val member = invocation.getArgument<AuthMember>(0)
                member
            }

            val response = handler.handle(request)

            assertTrue(response.id.isNotEmpty())
            assertTrue(response.id != request.name) // ID should be different from name
        }

    @Test
    fun `handle should generate JWT token`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                RegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )

            whenever(authMemberRepository.findByEmail(request.email)).thenReturn(null)
            whenever(authMemberRepository.create(any())).thenAnswer { invocation ->
                val member = invocation.getArgument<AuthMember>(0)
                member
            }

            val response = handler.handle(request)

            assertTrue(response.accessToken.isNotEmpty())
            assertTrue(response.refreshToken.isNotEmpty())
            assertTrue(response.accessToken != request.name) // Token should be different from name
        }
}
