package com.ohana.api.task.models

import com.ohana.shared.Guid
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class TaskUpdateRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val validTagIds = listOf(Guid.generate(), Guid.generate())
            val dueDate = Instant.now().plusSeconds(3600)

            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = dueDate,
                    status = "IN_PROGRESS",
                    tagIds = validTagIds,
                )

            val domainRequest = request.toDomain()

            assertEquals("Updated Task", domainRequest.title)
            assertEquals("An updated task description", domainRequest.description)
            assertEquals(dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.IN_PROGRESS, domainRequest.status)
            assertEquals(validTagIds, domainRequest.tagIds)
        }

    @Test
    fun `toDomain should pass when optional fields are null`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = null,
                )

            val domainRequest = request.toDomain()

            assertEquals("Updated Task", domainRequest.title)
            assertEquals("An updated task description", domainRequest.description)
            assertEquals(null, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should throw ValidationException when title is null`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = null,
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is blank`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when title is too long`() =
        runTest {
            val longTitle = "a".repeat(256)
            val request =
                TaskUpdateRequest(
                    title = longTitle,
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("title", exception.errors!![0].field)
            assertEquals("Title is required and must be between 1 and 255 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is null`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = null,
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Description is required and must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when description is too long`() =
        runTest {
            val longDescription = "a".repeat(1001)
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = longDescription,
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("description", exception.errors!![0].field)
            assertEquals("Description is required and must be at most 1000 characters long", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when status is null`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = null,
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("status", exception.errors!![0].field)
            assertEquals("Status is required and must be one of: PENDING, IN_PROGRESS, COMPLETED", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when status is invalid`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "INVALID_STATUS",
                    tagIds = emptyList(),
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
    fun `toDomain should throw ValidationException when tagIds contains invalid GUID`() =
        runTest {
            val invalidTagIds = listOf(Guid.generate(), "invalid-guid", Guid.generate())
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = invalidTagIds,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("tagIds", exception.errors!![0].field)
            assertEquals("All tag IDs must be valid GUIDs", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val longTitle = "a".repeat(256)
            val longDescription = "a".repeat(1001)
            val invalidTagIds = listOf("invalid-guid-1", "invalid-guid-2")
            val request =
                TaskUpdateRequest(
                    title = longTitle,
                    description = longDescription,
                    dueDate = null,
                    status = "INVALID_STATUS",
                    tagIds = invalidTagIds,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(4, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("title", "description", "status", "tagIds"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid status values`() =
        runTest {
            val validStatuses = listOf("PENDING", "IN_PROGRESS", "COMPLETED")

            validStatuses.forEach { status ->
                val request =
                    TaskUpdateRequest(
                        title = "Updated Task",
                        description = "An updated task description",
                        dueDate = null,
                        status = status,
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(TaskStatus.valueOf(status), domainRequest.status)
            }
        }

    @Test
    fun `toDomain should accept valid GUID formats for tagIds`() =
        runTest {
            val validTagIds = listOf(Guid.generate(), Guid.generate(), Guid.generate())
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = validTagIds,
                )

            val domainRequest = request.toDomain()
            assertEquals(validTagIds, domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle null tagIds correctly`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle empty tagIds correctly`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "PENDING",
                    tagIds = emptyList(),
                )

            val domainRequest = request.toDomain()
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle case sensitivity for status values`() =
        runTest {
            val request =
                TaskUpdateRequest(
                    title = "Updated Task",
                    description = "An updated task description",
                    dueDate = null,
                    status = "pending", // lowercase
                    tagIds = emptyList(),
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
    fun `toDomain should accept valid title formats`() =
        runTest {
            val validTitles =
                listOf(
                    "Simple Task",
                    "Complex Task with Special Characters!",
                    "Task with Numbers 123",
                    "a".repeat(255), // Maximum length
                )

            validTitles.forEach { title ->
                val request =
                    TaskUpdateRequest(
                        title = title,
                        description = "An updated task description",
                        dueDate = null,
                        status = "PENDING",
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(title, domainRequest.title)
            }
        }

    @Test
    fun `toDomain should accept valid description formats`() =
        runTest {
            val validDescriptions =
                listOf(
                    "Simple description",
                    "Complex description with special characters: !@#$%^&*()",
                    "Description with numbers 123 and symbols",
                    "", // Empty description is allowed
                    "a".repeat(1000), // Maximum length
                )

            validDescriptions.forEach { description ->
                val request =
                    TaskUpdateRequest(
                        title = "Updated Task",
                        description = description,
                        dueDate = null,
                        status = "PENDING",
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(description, domainRequest.description)
            }
        }
}
