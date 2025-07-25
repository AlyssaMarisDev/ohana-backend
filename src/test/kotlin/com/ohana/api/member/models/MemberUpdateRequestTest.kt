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
                    name = "Jane Doe",
                    age = null,
                    gender = null,
                )

            val domainRequest = request.toDomain()

            assertEquals("Jane Doe", domainRequest.name)
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
            assertEquals("Name is required", exception.errors!![0].message)
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
            assertEquals("Name cannot be blank", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too long`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "A".repeat(256), // 256 characters, exceeds 255 limit
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
            assertEquals("Name must be at most 255 characters long", exception.errors!![0].message)
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
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "", // Blank name
                    age = -5, // Negative age
                    gender = "", // Blank gender
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
    fun `toDomain should accept name with exactly 255 characters`() =
        runTest {
            val request =
                MemberUpdateRequest(
                    name = "A".repeat(255), // Exactly 255 characters
                    age = 30,
                    gender = "Male",
                )

            val domainRequest = request.toDomain()

            assertEquals("A".repeat(255), domainRequest.name)
            assertEquals(30, domainRequest.age)
            assertEquals("Male", domainRequest.gender)
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
}
