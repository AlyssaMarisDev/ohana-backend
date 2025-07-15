package com.ohana.auth.handlers

import com.ohana.TestUtils
import com.ohana.auth.entities.AuthMember
import com.ohana.auth.utils.Hasher
import com.ohana.exceptions.ConflictException
import com.ohana.shared.AuthMemberRepository
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
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

class MemberRegistrationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var authMemberRepository: AuthMemberRepository
    private lateinit var handler: MemberRegistrationHandler
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        authMemberRepository = mock()
        context =
            mock {
                on { authMembers } doReturn authMemberRepository
            }
        unitOfWork = mock()
        handler = MemberRegistrationHandler(unitOfWork)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should register member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                MemberRegistrationHandler.Request(
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

            val response = handler.handle(request)

            assertEquals(authMember.id, response.id)
            assertTrue(response.token.isNotEmpty())

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
            verifyNoMoreInteractions(authMemberRepository)
        }

    @Test
    fun `handle should throw ConflictException when email already exists`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                MemberRegistrationHandler.Request(
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
                MemberRegistrationHandler.Request(
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
    fun `handle should throw ValidationException when name is empty`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Name is required"))
        }

    @Test
    fun `handle should throw ValidationException when name is too short`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "AB", // 2 characters, less than minimum 3
                    email = "test@example.com",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Name must be at least 3 characters long"))
        }

    @Test
    fun `handle should throw ValidationException when email is empty`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
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
                MemberRegistrationHandler.Request(
                    name = "Test User",
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
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password is required"))
        }

    @Test
    fun `handle should throw ValidationException when password is too short`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "Short1!", // 7 characters, less than minimum 8
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password must be at least 8 characters long"))
        }

    @Test
    fun `handle should throw ValidationException when password lacks uppercase letter`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "validpass123!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password must contain at least one uppercase letter"))
        }

    @Test
    fun `handle should throw ValidationException when password lacks number`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass!",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password must contain at least one number"))
        }

    @Test
    fun `handle should throw ValidationException when password lacks special character`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("password" to "Password must contain at least one special character"))
        }

    @Test
    fun `handle should accept valid password with all requirements`() =
        runTest {
            val request =
                MemberRegistrationHandler.Request(
                    name = "Test User",
                    email = "test@example.com",
                    password = "ValidPass123!",
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should hash password correctly`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                MemberRegistrationHandler.Request(
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
                MemberRegistrationHandler.Request(
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
                MemberRegistrationHandler.Request(
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

            assertTrue(response.token.isNotEmpty())
            // Token should be different from ID
            assertTrue(response.token != response.id)
        }
}
