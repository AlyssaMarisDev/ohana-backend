package com.ohana.api.task.models

import com.ohana.shared.Guid
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import kotlin.test.assertEquals

class TaskGetAllRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val validHouseholdIds = listOf(Guid.generate(), Guid.generate())
            val request =
                TaskGetAllRequest(
                    householdIds = validHouseholdIds,
                    dueDateFrom = "2023-12-01T10:00:00Z",
                    dueDateTo = "2023-12-31T23:59:59Z",
                    completedDateFrom = "2023-12-01T00:00:00Z",
                    completedDateTo = "2023-12-31T23:59:59Z",
                )

            val domainRequest = request.toDomain()

            assertEquals(validHouseholdIds, domainRequest.householdIds)
            assertEquals(Instant.parse("2023-12-01T10:00:00Z"), domainRequest.dueDateFrom)
            assertEquals(Instant.parse("2023-12-31T23:59:59Z"), domainRequest.dueDateTo)
            assertEquals(Instant.parse("2023-12-01T00:00:00Z"), domainRequest.completedDateFrom)
            assertEquals(Instant.parse("2023-12-31T23:59:59Z"), domainRequest.completedDateTo)
        }

    @Test
    fun `toDomain should pass when optional date fields are null`() =
        runTest {
            val validHouseholdIds = listOf(Guid.generate())
            val request =
                TaskGetAllRequest(
                    householdIds = validHouseholdIds,
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val domainRequest = request.toDomain()

            assertEquals(validHouseholdIds, domainRequest.householdIds)
            assertEquals(null, domainRequest.dueDateFrom)
            assertEquals(null, domainRequest.dueDateTo)
            assertEquals(null, domainRequest.completedDateFrom)
            assertEquals(null, domainRequest.completedDateTo)
        }

    @Test
    fun `toDomain should throw ValidationException when householdIds contains invalid GUID`() =
        runTest {
            val invalidHouseholdIds = listOf(Guid.generate(), "invalid-guid", Guid.generate())
            val request =
                TaskGetAllRequest(
                    householdIds = invalidHouseholdIds,
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdIds", exception.errors!![0].field)
            assertEquals("All household IDs must be valid GUIDs", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when dueDateFrom is invalid format`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = "invalid-date",
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("dueDateFrom", exception.errors!![0].field)
            assertEquals("Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when dueDateTo is invalid format`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = null,
                    dueDateTo = "invalid-date",
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("dueDateTo", exception.errors!![0].field)
            assertEquals("Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when completedDateFrom is invalid format`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = "invalid-date",
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("completedDateFrom", exception.errors!![0].field)
            assertEquals("Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when completedDateTo is invalid format`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = "invalid-date",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("completedDateTo", exception.errors!![0].field)
            assertEquals("Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when dueDateFrom is after dueDateTo`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = "2023-12-31T23:59:59Z",
                    dueDateTo = "2023-12-01T10:00:00Z",
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("dueDateRange", exception.errors!![0].field)
            assertEquals("Due date 'from' must be before or equal to 'to'", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when completedDateFrom is after completedDateTo`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = "2023-12-31T23:59:59Z",
                    completedDateTo = "2023-12-01T10:00:00Z",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("completedDateRange", exception.errors!![0].field)
            assertEquals("Completed date 'from' must be before or equal to 'to'", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val invalidHouseholdIds = listOf("invalid-guid-1", "invalid-guid-2")
            val request =
                TaskGetAllRequest(
                    householdIds = invalidHouseholdIds,
                    dueDateFrom = "invalid-date-1",
                    dueDateTo = "invalid-date-2",
                    completedDateFrom = "invalid-date-3",
                    completedDateTo = "invalid-date-4",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(5, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("householdIds", "dueDateFrom", "dueDateTo", "completedDateFrom", "completedDateTo"), errorFields)
        }

    @Test
    fun `toDomain should accept valid GUID formats for householdIds`() =
        runTest {
            val validHouseholdIds = listOf(Guid.generate(), Guid.generate(), Guid.generate())
            val request =
                TaskGetAllRequest(
                    householdIds = validHouseholdIds,
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(validHouseholdIds, domainRequest.householdIds)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "2023-12-01T10:00:00Z",
            "2023-12-01T10:00:00.000Z",
            "2023-12-01T10:00:00.123Z",
        ],
    )
    fun `toDomain should accept valid ISO 8601 date formats`(date: String) =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = date,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(Instant.parse(date), domainRequest.dueDateFrom)
        }

    @Test
    fun `toDomain should accept equal dates for date ranges`() =
        runTest {
            val sameDate = "2023-12-01T10:00:00Z"
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = sameDate,
                    dueDateTo = sameDate,
                    completedDateFrom = sameDate,
                    completedDateTo = sameDate,
                )

            val domainRequest = request.toDomain()
            assertEquals(Instant.parse(sameDate), domainRequest.dueDateFrom)
            assertEquals(Instant.parse(sameDate), domainRequest.dueDateTo)
            assertEquals(Instant.parse(sameDate), domainRequest.completedDateFrom)
            assertEquals(Instant.parse(sameDate), domainRequest.completedDateTo)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "2023-12-01", // Missing time
            "10:00:00", // Missing date
            "2023/12/01T10:00:00Z", // Wrong date separator
            "2023-12-01 10:00:00Z", // Wrong time separator
            "2023-12-01T10:00:00", // Missing timezone
            "invalid-date-string",
            "",
            "2023-13-01T10:00:00Z", // Invalid month
            "2023-12-32T10:00:00Z", // Invalid day
            "2023-12-01T25:00:00Z", // Invalid hour
        ],
    )
    fun `toDomain should handle various invalid date formats`(invalidDate: String) =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(Guid.generate()),
                    dueDateFrom = invalidDate,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("dueDateFrom", exception.errors!![0].field)
            assertEquals("Date must be in ISO 8601 format (e.g., 2023-12-01T10:00:00Z)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should handle empty householdIds list`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = emptyList(),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(emptyList<String>(), domainRequest.householdIds)
        }

    @Test
    fun `toDomain should handle single household ID`() =
        runTest {
            val singleHouseholdId = Guid.generate()
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(singleHouseholdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(listOf(singleHouseholdId), domainRequest.householdIds)
        }
}
