package com.ohana.api.household.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class HouseholdCreationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "Test Household",
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()

            assertEquals("550e8400-e29b-41d4-a716-446655440000", domainRequest.id)
            assertEquals("Test Household", domainRequest.name)
            assertEquals("A test household description", domainRequest.description)
        }

    @Test
    fun `toDomain should throw ValidationException when id is null`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = null,
                    name = "Test Household",
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("id", exception.errors!![0].field)
            assertEquals("Household ID is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when id is blank`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "",
                    name = "Test Household",
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("id", exception.errors!![0].field)
            assertEquals("Household ID cannot be blank", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when id is not a valid GUID`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "invalid-guid",
                    name = "Test Household",
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("id", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is null`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = null,
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Household name is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is blank`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "",
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Household name cannot be blank", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too long`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "A".repeat(256), // 256 characters, exceeds 255 limit
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Household name must be at most 255 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is null`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "Test Household",
                    description = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Household description is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should pass when description is empty`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "Test Household",
                    description = "",
                )

            val domainRequest = request.toDomain()

            assertEquals("550e8400-e29b-41d4-a716-446655440000", domainRequest.id)
            assertEquals("Test Household", domainRequest.name)
            assertEquals("", domainRequest.description)
        }

    @Test
    fun `toDomain should throw ValidationException when description is too long`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "Test Household",
                    description = "A".repeat(1001), // 1001 characters, exceeds 1000 limit
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Household description must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "", // Blank id
                    name = "", // Blank name
                    description = null, // Missing description
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(3, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("id", "name", "description"), errorFields)
        }

    @Test
    fun `toDomain should accept name with exactly 255 characters`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "A".repeat(255), // Exactly 255 characters
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()

            assertEquals("A".repeat(255), domainRequest.name)
        }

    @Test
    fun `toDomain should accept description with exactly 1000 characters`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    name = "Test Household",
                    description = "A".repeat(1000), // Exactly 1000 characters
                )

            val domainRequest = request.toDomain()

            assertEquals("A".repeat(1000), domainRequest.description)
        }

    @Test
    fun `toDomain should accept valid GUID formats`() =
        runTest {
            val validGuids =
                listOf(
                    "550e8400-e29b-41d4-a716-446655440000",
                    "550e8400-e29b-41d4-a716-446655440001",
                    "550e8400-e29b-41d4-a716-446655440002",
                )

            validGuids.forEach { guid ->
                val request =
                    HouseholdCreationRequest(
                        id = guid,
                        name = "Test Household",
                        description = "A test household description",
                    )

                val domainRequest = request.toDomain()
                assertEquals(guid, domainRequest.id)
            }
        }
}
