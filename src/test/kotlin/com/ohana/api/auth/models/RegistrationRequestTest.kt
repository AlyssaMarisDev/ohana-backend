package com.ohana.api.auth.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RegistrationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = "SecurePass123",
                )

            val domainRequest = request.toDomain()

            assertEquals("John Doe", domainRequest.name)
            assertEquals("john.doe@example.com", domainRequest.email)
            assertEquals("SecurePass123", domainRequest.password)
        }

    @Test
    fun `toDomain should throw ValidationException when name is null`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = null,
                    email = "john.doe@example.com",
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be at least 3 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is blank`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "",
                    email = "john.doe@example.com",
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be at least 3 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too short`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "Jo",
                    email = "john.doe@example.com",
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be at least 3 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when email is null`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = null,
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("email", exception.errors!![0].field)
            assertEquals("Email is required and must be in a valid format", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when email is blank`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "",
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("email", exception.errors!![0].field)
            assertEquals("Email is required and must be in a valid format", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when email format is invalid`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "invalid-email",
                    password = "SecurePass123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("email", exception.errors!![0].field)
            assertEquals("Email is required and must be in a valid format", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when password is null`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("password", exception.errors!![0].field)
            assertEquals(
                "Password is required and must be at least 8 characters long with at least one uppercase letter and one number",
                exception.errors!![0].message,
            )
        }

    @Test
    fun `toDomain should throw ValidationException when password is blank`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("password", exception.errors!![0].field)
            assertEquals(
                "Password is required and must be at least 8 characters long with at least one uppercase letter and one number",
                exception.errors!![0].message,
            )
        }

    @Test
    fun `toDomain should throw ValidationException when password is too short`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = "Pass1",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("password", exception.errors!![0].field)
            assertEquals(
                "Password is required and must be at least 8 characters long with at least one uppercase letter and one number",
                exception.errors!![0].message,
            )
        }

    @Test
    fun `toDomain should throw ValidationException when password has no uppercase letter`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = "password123",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("password", exception.errors!![0].field)
            assertEquals(
                "Password is required and must be at least 8 characters long with at least one uppercase letter and one number",
                exception.errors!![0].message,
            )
        }

    @Test
    fun `toDomain should throw ValidationException when password has no number`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    password = "PasswordABC",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("password", exception.errors!![0].field)
            assertEquals(
                "Password is required and must be at least 8 characters long with at least one uppercase letter and one number",
                exception.errors!![0].message,
            )
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                RegistrationRequest(
                    name = "Jo", // Too short
                    email = "invalid-email", // Invalid email
                    password = "pass", // Too short, no uppercase, no number
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(3, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("name", "email", "password"), errorFields)
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
                    "user123@example.co.uk",
                )

            validEmails.forEach { email ->
                val request =
                    RegistrationRequest(
                        name = "John Doe",
                        email = email,
                        password = "SecurePass123",
                    )

                val domainRequest = request.toDomain()
                assertEquals(email, domainRequest.email)
            }
        }

    @Test
    fun `toDomain should accept valid password formats`() =
        runTest {
            val validPasswords =
                listOf(
                    "SecurePass123",
                    "MyPassword1",
                    "ComplexP@ss1",
                    "Test123ABC",
                )

            validPasswords.forEach { password ->
                val request =
                    RegistrationRequest(
                        name = "John Doe",
                        email = "john.doe@example.com",
                        password = password,
                    )

                val domainRequest = request.toDomain()
                assertEquals(password, domainRequest.password)
            }
        }

    @Test
    fun `toDomain should accept valid name formats`() =
        runTest {
            val validNames =
                listOf(
                    "John Doe",
                    "Mary Jane",
                    "Dr. Smith",
                    "O'Connor",
                    "José García",
                )

            validNames.forEach { name ->
                val request =
                    RegistrationRequest(
                        name = name,
                        email = "john.doe@example.com",
                        password = "SecurePass123",
                    )

                val domainRequest = request.toDomain()
                assertEquals(name, domainRequest.name)
            }
        }
}
