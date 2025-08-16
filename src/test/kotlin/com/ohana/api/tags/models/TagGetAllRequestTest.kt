package com.ohana.api.tags.models

import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class TagGetAllRequestTest {
    @Test
    fun `toDomain should pass when householdId is valid`() =
        runTest {
            val validHouseholdId = Guid.generate()
            val request =
                TagGetAllRequest(
                    householdId = validHouseholdId,
                )

            val domainRequest = request.toDomain()

            assertEquals(validHouseholdId, domainRequest.householdId)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is invalid GUID`() =
        runTest {
            val request =
                TagGetAllRequest(
                    householdId = "invalid-guid",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdId", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is empty string`() =
        runTest {
            val request =
                TagGetAllRequest(
                    householdId = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdId", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "not-a-guid",
            "12345678-1234-1234-1234-1234567890123", // Too long
            "12345678-1234-1234-1234-12345678901", // Too short
            "12345678-1234-1234-1234-12345678901g", // Invalid character
            "12345678-1234-1234-1234-12345678901G", // Invalid character
            "12345678-1234-1234-1234-12345678901X", // Invalid character
            "12345678-1234-1234-123456789012", // Missing hyphens
            "12345678123412341234123456789012", // No hyphens at all
        ],
    )
    fun `toDomain should throw ValidationException when householdId is malformed`(malformedGuid: String) =
        runTest {
            val request =
                TagGetAllRequest(
                    householdId = malformedGuid,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdId", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @RepeatedTest(3)
    fun `toDomain should accept valid GUID formats`() =
        runTest {
            val guid = Guid.generate()
            val request =
                TagGetAllRequest(
                    householdId = guid,
                )

            val domainRequest = request.toDomain()
            assertEquals(guid, domainRequest.householdId)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "00000000-0000-0000-0000-000000000000", // All zeros
            "ffffffff-ffff-ffff-ffff-ffffffffffff", // All f's
            "12345678-1234-1234-1234-123456789abc", // Mixed case
            "ABCDEF12-3456-7890-ABCD-EF1234567890", // All uppercase
        ],
    )
    fun `toDomain should handle edge case GUIDs`(guid: String) =
        runTest {
            val request =
                TagGetAllRequest(
                    householdId = guid,
                )

            val domainRequest = request.toDomain()
            assertEquals(guid, domainRequest.householdId)
        }
}
