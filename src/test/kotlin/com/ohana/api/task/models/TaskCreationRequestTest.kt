package com.ohana.api.task.models

import com.ohana.shared.Guid
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TaskCreationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val domainRequest = request.toDomain()

            assertEquals("Valid Title", domainRequest.title)
            assertEquals("Valid description", domainRequest.description)
            assertEquals(request.dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
            assertEquals(request.householdId, domainRequest.householdId)
            Guid.isValid(domainRequest.id)
        }

    @Test
    fun `toDomain should throw ValidationException when title is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = null,
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("title", exception.errors[0].field)
            assertEquals("Title is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is blank`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("title", exception.errors[0].field)
            assertEquals("Title cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is too long`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "A".repeat(256), // 256 characters, exceeds 255 limit
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("title", exception.errors[0].field)
            assertEquals("Title must be at most 255 characters long", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = null,
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("description", exception.errors[0].field)
            assertEquals("Description is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is blank`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("description", exception.errors[0].field)
            assertEquals("Description cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is too long`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "A".repeat(1001), // 1001 characters, exceeds 1000 limit
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("description", exception.errors[0].field)
            assertEquals("Description must be at most 1000 characters long", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when due date is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("dueDate", exception.errors[0].field)
            assertEquals("Due date is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when due date is in the past`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().minusSeconds(3600), // 1 hour ago
                    status = "PENDING",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("dueDate", exception.errors[0].field)
            assertEquals("Due date cannot be in the past", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when status is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = null,
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("status", exception.errors[0].field)
            assertEquals("Status is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when status is invalid`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "INVALID_STATUS",
                    householdId = Guid.generate(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("status", exception.errors[0].field)
            assertEquals("Status must be one of: PENDING, IN_PROGRESS, COMPLETED", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("householdId", exception.errors[0].field)
            assertEquals("Household ID is required", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is blank`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("householdId", exception.errors[0].field)
            assertEquals("Household ID cannot be blank", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is invalid GUID`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                    householdId = "invalid-guid",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors.size)
            assertEquals("householdId", exception.errors[0].field)
            assertEquals("Household ID must be a valid GUID", exception.errors[0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "", // Blank title
                    description = "A".repeat(1001), // Too long description
                    dueDate = null, // Missing due date
                    status = "INVALID_STATUS", // Invalid status
                    householdId = null, // Missing household ID
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(5, exception.errors.size)

            val errorFields = exception.errors.map { it.field }.toSet()
            assertEquals(setOf("title", "description", "dueDate", "status", "householdId"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid status values`() =
        runTest {
            val validStatuses = listOf("PENDING", "IN_PROGRESS", "COMPLETED")

            validStatuses.forEach { status ->
                val request =
                    TaskCreationRequest(
                        title = "Valid Title",
                        description = "Valid description",
                        dueDate = Instant.now().plusSeconds(3600),
                        status = status,
                        householdId = Guid.generate(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(TaskStatus.valueOf(status), domainRequest.status)
            }
        }
}
