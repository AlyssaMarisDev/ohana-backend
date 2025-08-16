package com.ohana.api.household.models

import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class HouseholdCreationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val validId = Guid.generate()
            val request =
                HouseholdCreationRequest(
                    id = validId,
                    name = "Test Household",
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()

            assertEquals(validId, domainRequest.id)
            assertEquals("Test Household", domainRequest.name)
            assertEquals("A test household description", domainRequest.description)
        }

    @Test
    fun `toDomain should generate new ID when id is not provided`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    name = "Test Household",
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()

            // Should generate a new valid GUID
            assertEquals(true, Guid.isValid(domainRequest.id))
            assertEquals("Test Household", domainRequest.name)
            assertEquals("A test household description", domainRequest.description)
        }

    @Test
    fun `toDomain should throw ValidationException when id is invalid GUID`() =
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
            assertEquals("Id must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is null`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
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
            assertEquals("Household name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is blank`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
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
            assertEquals("Household name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too long`() =
        runTest {
            val longName = "a".repeat(256)
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = longName,
                    description = "A test household description",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Household name is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is null`() =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
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
            assertEquals("Household description is required and must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is too long`() =
        runTest {
            val longDescription = "a".repeat(1001)
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = "Test Household",
                    description = longDescription,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Household description is required and must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val longName = "a".repeat(256)
            val longDescription = "a".repeat(1001)
            val request =
                HouseholdCreationRequest(
                    id = "invalid-guid",
                    name = longName,
                    description = longDescription,
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

    @ParameterizedTest
    @ValueSource(
        strings = [
            "My Household",
            "Family Home",
            "Apartment 123",
            "Beach House",
            "Mountain Cabin",
        ],
    )
    fun `toDomain should accept valid name formats`(name: String) =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = name,
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()
            assertEquals(name, domainRequest.name)
        }

    @Test
    fun `toDomain should accept maximum length name`() =
        runTest {
            val maxLengthName = "a".repeat(255)
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = maxLengthName,
                    description = "A test household description",
                )

            val domainRequest = request.toDomain()
            assertEquals(maxLengthName, domainRequest.name)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "A cozy family home",
            "Our primary residence",
            "Vacation property",
            "", // Empty description is allowed
        ],
    )
    fun `toDomain should accept valid description formats`(description: String) =
        runTest {
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = "Test Household",
                    description = description,
                )

            val domainRequest = request.toDomain()
            assertEquals(description, domainRequest.description)
        }

    @Test
    fun `toDomain should accept maximum length description`() =
        runTest {
            val maxLengthDescription = "a".repeat(1000)
            val request =
                HouseholdCreationRequest(
                    id = Guid.generate(),
                    name = "Test Household",
                    description = maxLengthDescription,
                )

            val domainRequest = request.toDomain()
            assertEquals(maxLengthDescription, domainRequest.description)
        }

    @RepeatedTest(3)
    fun `toDomain should accept valid GUID formats`() =
        runTest {
            val guid = Guid.generate()
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
