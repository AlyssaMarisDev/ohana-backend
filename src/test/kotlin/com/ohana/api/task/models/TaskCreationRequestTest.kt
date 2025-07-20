package com.ohana.api.task.models

import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

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
                )

            val domainRequest = request.toDomain()

            assertEquals("Valid Title", domainRequest.title)
            assertEquals("Valid description", domainRequest.description)
            assertEquals(request.dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
            // ID should be a valid UUID
            UUID.fromString(domainRequest.id) // This will throw if invalid
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
    fun `toDomain should throw ValidationException when title is blank`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
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
    fun `toDomain should throw ValidationException when description is blank`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
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
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "", // Blank title
                    description = "A".repeat(1001), // Too long description
                    dueDate = null, // Missing due date
                    status = "INVALID_STATUS", // Invalid status
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(4, exception.errors.size)

            val errorFields = exception.errors.map { it.field }.toSet()
            assertEquals(setOf("title", "description", "dueDate", "status"), errorFields)
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
                    )

                val domainRequest = request.toDomain()
                assertEquals(TaskStatus.valueOf(status), domainRequest.status)
            }
        }

    @Test
    fun `toDomain should generate unique IDs for each call`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                )

            val domainRequest1 = request.toDomain()
            val domainRequest2 = request.toDomain()

            // IDs should be different
            assert(domainRequest1.id != domainRequest2.id)

            // Both should be valid UUIDs
            UUID.fromString(domainRequest1.id)
            UUID.fromString(domainRequest2.id)
        }
}
