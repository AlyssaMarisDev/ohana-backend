package com.ohana.api.member.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MemberUpdateRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = 30,
                    gender = "Male",
                )

            val domainRequest = request.toDomain()

            assertEquals("John Doe", domainRequest.name)
            assertEquals(30, domainRequest.age)
            assertEquals("Male", domainRequest.gender)
        }

    @Test
    fun `toDomain should pass when optional fields are null`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = null,
                    gender = null,
                )

            val domainRequest = request.toDomain()

            assertEquals("John Doe", domainRequest.name)
            assertEquals(null, domainRequest.age)
            assertEquals(null, domainRequest.gender)
        }

    @Test
    fun `toDomain should throw ValidationException when name is null`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = null,
                    age = 30,
                    gender = "Male",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is blank`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "",
                    age = 30,
                    gender = "Male",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too long`() =
        runTest {
            val longName = "a".repeat(256)
            val request =
                MemberUpdateRequest(
                    name = longName,
                    age = 30,
                    gender = "Male",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when age is negative`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = -1,
                    gender = "Male",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("age", exception.errors!![0].field)
            assertEquals("Age must be a positive number", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should accept zero age`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = 0,
                    gender = "Male",
                )

            val domainRequest = request.toDomain()

            assertEquals("John Doe", domainRequest.name)
            assertEquals(0, domainRequest.age)
            assertEquals("Male", domainRequest.gender)
        }

    @Test
    fun `toDomain should throw ValidationException when gender is blank`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = 30,
                    gender = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("gender", exception.errors!![0].field)
            assertEquals("Gender cannot be blank if provided", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when gender is whitespace only`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = 30,
                    gender = "   ",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("gender", exception.errors!![0].field)
            assertEquals("Gender cannot be blank if provided", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val longName = "a".repeat(256)
            val request =
                MemberUpdateRequest(
                    name = longName,
                    age = -5,
                    gender = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(3, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("name", "age", "gender"), errorFields)
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
                    "a".repeat(255), // Maximum length
                )

            validNames.forEach { name ->
                val request =
                    MemberUpdateRequest(
                        name = name,
                        age = 30,
                        gender = "Male",
                    )

                val domainRequest = request.toDomain()
                assertEquals(name, domainRequest.name)
            }
        }

    @Test
    fun `toDomain should accept valid age values`() =
        runTest {
            val validAges = listOf(1, 18, 25, 50, 100, 120)

            validAges.forEach { age ->
                val request =
                    MemberUpdateRequest(
                        name = "John Doe",
                        age = age,
                        gender = "Male",
                    )

                val domainRequest = request.toDomain()
                assertEquals(age, domainRequest.age)
            }
        }

    @Test
    fun `toDomain should accept valid gender values`() =
        runTest {
            val validGenders = listOf("Male", "Female", "Other", "Prefer not to say", "Non-binary")

            validGenders.forEach { gender ->
                val request =
                    MemberUpdateRequest(
                        name = "John Doe",
                        age = 30,
                        gender = gender,
                    )

                val domainRequest = request.toDomain()
                assertEquals(gender, domainRequest.gender)
            }
        }

    @Test
    fun `toDomain should accept null age and gender`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = null,
                    gender = null,
                )

            val domainRequest = request.toDomain()
            assertEquals("John Doe", domainRequest.name)
            assertEquals(null, domainRequest.age)
            assertEquals(null, domainRequest.gender)
        }

    @Test
    fun `toDomain should accept zero age when age is null`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "John Doe",
                    age = null,
                    gender = "Male",
                )

            val domainRequest = request.toDomain()
            assertEquals("John Doe", domainRequest.name)
            assertEquals(null, domainRequest.age)
            assertEquals("Male", domainRequest.gender)
        }
}
