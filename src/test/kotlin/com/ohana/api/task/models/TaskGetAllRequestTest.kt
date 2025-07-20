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
            assertEquals(1, exception.errors.size)
            assertEquals("householdIds[1]", exception.errors[0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors[0].message)
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
            assertEquals(1, exception.errors.size)
            assertEquals("householdIds[1]", exception.errors[0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors[0].message)
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
            assertEquals(2, exception.errors.size)

            val errorFields = exception.errors.map { it.field }.toSet()
            assertEquals(setOf("householdIds[0]", "householdIds[1]"), errorFields)

            exception.errors.forEach { error ->
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
            assertEquals(1, exception.errors.size)
            assertEquals("householdIds[1]", exception.errors[0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors[0].message)
        }
}
