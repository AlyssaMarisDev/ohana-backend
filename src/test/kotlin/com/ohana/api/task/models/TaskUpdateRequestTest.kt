package com.ohana.api.task.models

import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TaskUpdateRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Valid Title",
                    description = "Valid description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = "PENDING",
                )

            // Should not throw any exception
            val domainRequest = request.toDomain()

            assertEquals("Valid Title", domainRequest.title)
            assertEquals("Valid description", domainRequest.description)
            assertEquals(request.dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
        }

    @Test
    fun `toDomain should throw ValidationException when title is null`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is null`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Description is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is blank`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title cannot be blank", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is too long`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title must be at most 255 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is too long`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Description must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when status is invalid`() =
        runTest {
            val request =
                TaskUpdateRequest(
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
            assertEquals(1, exception.errors!!.size)
            assertEquals("status", exception.errors!![0].field)
            assertEquals("Status must be one of: PENDING, IN_PROGRESS, COMPLETED", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "", // Blank title
                    description = "A".repeat(1001), // Too long description
                    dueDate = null,
                    status = "INVALID_STATUS", // Invalid status
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(4, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("title", "description", "dueDate", "status"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid status values`() =
        runTest {
            val validStatuses = listOf("PENDING", "IN_PROGRESS", "COMPLETED")

            validStatuses.forEach { status ->
                val request =
                    TaskUpdateRequest(
                        title = "Valid Title",
                        description = "Valid description",
                        dueDate = Instant.now().plusSeconds(3600),
                        status = status,
                    )

                // Should not throw any exception
                val domainRequest = request.toDomain()
                assertEquals(TaskStatus.valueOf(status), domainRequest.status)
            }
        }

    @Test
    fun `toDomain should return correct domain object when all fields are valid`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "New Title",
                    description = "New Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = "IN_PROGRESS",
                )

            val domainRequest = request.toDomain()

            assertEquals("New Title", domainRequest.title)
            assertEquals("New Description", domainRequest.description)
            assertEquals(request.dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.IN_PROGRESS, domainRequest.status)
        }
}
