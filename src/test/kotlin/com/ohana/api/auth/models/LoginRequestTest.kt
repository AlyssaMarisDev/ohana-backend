package com.ohana.api.auth.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LoginRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                LoginRequest(
                    email = "john.doe@example.com",
                    password = "SecurePass123!",
                )

            val domainRequest = request.toDomain()

            assertEquals("john.doe@example.com", domainRequest.email)
            assertEquals("SecurePass123!", domainRequest.password)
        }

    @Test
    fun `toDomain should throw ValidationException when email is null`() =
        runTest {
            val request =
                LoginRequest(
                    email = null,
                    password = "SecurePass123!",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("email", exception.errors[0].field)
            assertEquals("Email is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when email is blank`() =
        runTest {
            val request =
                LoginRequest(
                    email = "",
                    password = "SecurePass123!",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("email", exception.errors[0].field)
            assertEquals("Email cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when email format is invalid`() =
        runTest {
            val request =
                LoginRequest(
                    email = "invalid-email",
                    password = "SecurePass123!",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("email", exception.errors[0].field)
            assertEquals("Invalid email format", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when password is null`() =
        runTest {
            val request =
                LoginRequest(
                    email = "john.doe@example.com",
                    password = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("password", exception.errors[0].field)
            assertEquals("Password is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when password is blank`() =
        runTest {
            val request =
                LoginRequest(
                    email = "john.doe@example.com",
                    password = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("password", exception.errors[0].field)
            assertEquals("Password cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                LoginRequest(
                    email = "invalid-email", // Invalid email
                    password = "", // Blank password
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(2, exception.errors.size)

            val errorFields = exception.errors.map { it.field }.toSet()
            assertEquals(setOf("email", "password"), errorFields)
        }

    @Test
    fun `toDomain should accept valid email formats`() =
        runTest {
            val validEmails =
                listOf(
                    "user@example.com",
                    "user.name@example.com",
                    "user+tag@example.com",
                    "user@subdomain.example.com",
                )

            validEmails.forEach { email ->
                val request =
                    LoginRequest(
                        email = email,
                        password = "SecurePass123!",
                    )

                val domainRequest = request.toDomain()
                assertEquals(email, domainRequest.email)
            }
        }

    @Test
    fun `toDomain should accept any non-blank password`() =
        runTest {
            val validPasswords =
                listOf(
                    "password",
                    "123456",
                    "SecurePass123!",
                    "a",
                )

            validPasswords.forEach { password ->
                val request =
                    LoginRequest(
                        email = "john.doe@example.com",
                        password = password,
                    )

                val domainRequest = request.toDomain()
                assertEquals(password, domainRequest.password)
            }
        }
}
