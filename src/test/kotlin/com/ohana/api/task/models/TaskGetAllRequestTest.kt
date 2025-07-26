package com.ohana.api.task.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TaskGetAllRequestTest {
    @Test
    fun `toDomain should pass when householdIds is empty list`() =
        runTest {
            val request = TaskGetAllRequest(householdIds = emptyList())
            val result = request.toDomain()
            assertEquals(emptyList<String>(), result.householdIds)
        }

    @Test
    fun `toDomain should pass when householdIds contains valid IDs`() =
        runTest {
            val householdIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
            val request = TaskGetAllRequest(householdIds = householdIds)
            val result = request.toDomain()
            assertEquals(householdIds, result.householdIds)
        }

    @Test
    fun `toDomain should throw ValidationException when householdIds contains empty string`() =
        runTest {
            val request = TaskGetAllRequest(householdIds = listOf(UUID.randomUUID().toString(), ""))

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdIds[1]", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdIds contains blank string`() =
        runTest {
            val request = TaskGetAllRequest(householdIds = listOf(UUID.randomUUID().toString(), "   "))

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdIds[1]", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdIds contains multiple blank strings`() =
        runTest {
            val request = TaskGetAllRequest(householdIds = listOf("", "   ", UUID.randomUUID().toString()))

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(2, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("householdIds[0]", "householdIds[1]"), errorFields)

            exception.errors!!.forEach { error ->
                assertEquals("Household ID must be a valid GUID", error.message)
            }
        }

    @Test
    fun `toDomain should handle single valid household ID`() =
        runTest {
            val householdIds = listOf(UUID.randomUUID().toString())
            val request = TaskGetAllRequest(householdIds = householdIds)
            val result = request.toDomain()
            assertEquals(householdIds, result.householdIds)
        }

    @Test
    fun `toDomain should handle mixed valid and invalid household IDs`() =
        runTest {
            val request = TaskGetAllRequest(householdIds = listOf(UUID.randomUUID().toString(), "", UUID.randomUUID().toString()))

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdIds[1]", exception.errors!![0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should pass when date range parameters are null`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )
            val result = request.toDomain()
            assertEquals(null, result.dueDateFrom)
            assertEquals(null, result.dueDateTo)
            assertEquals(null, result.completedDateFrom)
            assertEquals(null, result.completedDateTo)
        }

    @Test
    fun `toDomain should pass when valid ISO 8601 dates are provided`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateFrom = "2023-12-01T10:00:00Z",
                    dueDateTo = "2023-12-31T23:59:59Z",
                    completedDateFrom = "2023-12-15T00:00:00Z",
                    completedDateTo = "2023-12-15T23:59:59Z",
                )
            val result = request.toDomain()
            assertEquals("2023-12-01T10:00:00Z", result.dueDateFrom?.toString())
            assertEquals("2023-12-31T23:59:59Z", result.dueDateTo?.toString())
            assertEquals("2023-12-15T00:00:00Z", result.completedDateFrom?.toString())
            assertEquals("2023-12-15T23:59:59Z", result.completedDateTo?.toString())
        }

    @Test
    fun `toDomain should throw ValidationException when dueDateFrom is invalid format`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateFrom = "invalid-date",
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
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateTo = "2023/12/01",
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
                    householdIds = listOf(UUID.randomUUID().toString()),
                    completedDateFrom = "yesterday",
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
                    householdIds = listOf(UUID.randomUUID().toString()),
                    completedDateTo = "tomorrow",
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
    fun `toDomain should throw ValidationException when due date range is invalid`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateFrom = "2023-12-31T23:59:59Z",
                    dueDateTo = "2023-12-01T10:00:00Z",
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
    fun `toDomain should throw ValidationException when completed date range is invalid`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    completedDateFrom = "2023-12-15T23:59:59Z",
                    completedDateTo = "2023-12-15T00:00:00Z",
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
    fun `toDomain should pass when due date range is valid with same dates`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    dueDateFrom = "2023-12-01T10:00:00Z",
                    dueDateTo = "2023-12-01T10:00:00Z",
                )
            val result = request.toDomain()
            assertEquals("2023-12-01T10:00:00Z", result.dueDateFrom?.toString())
            assertEquals("2023-12-01T10:00:00Z", result.dueDateTo?.toString())
        }

    @Test
    fun `toDomain should pass when completed date range is valid with same dates`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf(UUID.randomUUID().toString()),
                    completedDateFrom = "2023-12-15T00:00:00Z",
                    completedDateTo = "2023-12-15T00:00:00Z",
                )
            val result = request.toDomain()
            assertEquals("2023-12-15T00:00:00Z", result.completedDateFrom?.toString())
            assertEquals("2023-12-15T00:00:00Z", result.completedDateTo?.toString())
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                TaskGetAllRequest(
                    householdIds = listOf("", UUID.randomUUID().toString()),
                    dueDateFrom = "invalid-date",
                    dueDateTo = "2023-12-01T10:00:00Z",
                    completedDateFrom = "2023-12-31T23:59:59Z",
                    completedDateTo = "2023-12-01T10:00:00Z",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(3, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("householdIds[0]", "dueDateFrom", "completedDateRange"), errorFields)
        }
}
