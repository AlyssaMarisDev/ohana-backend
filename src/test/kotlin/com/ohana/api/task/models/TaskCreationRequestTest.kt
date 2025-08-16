package com.ohana.api.task.models

import com.ohana.shared.Guid
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class TaskCreationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val validId = Guid.generate()
            val validHouseholdId = Guid.generate()
            val validTagIds = listOf(Guid.generate(), Guid.generate())
            val dueDate = Instant.now().plusSeconds(3600)

            val request =
                TaskCreationRequest(
                    id = validId,
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = dueDate,
                    status = "PENDING",
                    householdId = validHouseholdId,
                    tagIds = validTagIds,
                )

            val domainRequest = request.toDomain()

            assertEquals(validId, domainRequest.id)
            assertEquals("Test Task", domainRequest.title)
            assertEquals("A test task description", domainRequest.description)
            assertEquals(dueDate, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
            assertEquals(validHouseholdId, domainRequest.householdId)
            assertEquals(validTagIds, domainRequest.tagIds)
        }

    @Test
    fun `toDomain should use provided ID when valid`() =
        runTest {
            val validId = Guid.generate()
            val validHouseholdId = Guid.generate()
            val request =
                TaskCreationRequest(
                    id = validId,
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = validHouseholdId,
                    tagIds = emptyList(),
                )

            val domainRequest = request.toDomain()

            assertEquals(validId, domainRequest.id)
            assertEquals("Test Task", domainRequest.title)
            assertEquals("A test task description", domainRequest.description)
            assertEquals(null, domainRequest.dueDate)
            assertEquals(TaskStatus.PENDING, domainRequest.status)
            assertEquals(validHouseholdId, domainRequest.householdId)
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should throw ValidationException when id is invalid GUID`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = "invalid-guid",
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
                    tagIds = emptyList(),
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
    fun `toDomain should throw ValidationException when title is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = null,
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = longTitle,
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = null,
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = longDescription,
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = null,
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "INVALID_STATUS",
                    householdId = Guid.generate(),
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
    fun `toDomain should throw ValidationException when householdId is null`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = null,
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdId", exception.errors!![0].field)
            assertEquals("Household ID is required and must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when householdId is invalid GUID`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = "invalid-guid",
                    tagIds = emptyList(),
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("householdId", exception.errors!![0].field)
            assertEquals("Household ID is required and must be a valid GUID", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when tagIds contains invalid GUID`() =
        runTest {
            val invalidTagIds = listOf(Guid.generate(), "invalid-guid", Guid.generate())
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
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
                TaskCreationRequest(
                    id = "invalid-guid",
                    title = longTitle,
                    description = longDescription,
                    dueDate = null,
                    status = "INVALID_STATUS",
                    householdId = "invalid-household-guid",
                    tagIds = invalidTagIds,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(6, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("id", "title", "description", "status", "householdId", "tagIds"), errorFields)
        }

    @Test
    fun `toDomain should accept all valid status values`() =
        runTest {
            val validStatuses = listOf("PENDING", "IN_PROGRESS", "COMPLETED")
            val validId = Guid.generate()
            val validHouseholdId = Guid.generate()

            validStatuses.forEach { status ->
                val request =
                    TaskCreationRequest(
                        id = validId,
                        title = "Test Task",
                        description = "A test task description",
                        dueDate = null,
                        status = status,
                        householdId = validHouseholdId,
                        tagIds = emptyList(),
                    )

                val domainRequest = request.toDomain()
                assertEquals(TaskStatus.valueOf(status), domainRequest.status)
            }
        }

    @Test
    fun `toDomain should accept valid GUID formats`() =
        runTest {
            val validGuids = listOf(Guid.generate(), Guid.generate(), Guid.generate())

            validGuids.forEach { guid ->
                val request =
                    TaskCreationRequest(
                        id = guid,
                        title = "Test Task",
                        description = "A test task description",
                        dueDate = null,
                        status = "PENDING",
                        householdId = guid,
                        tagIds = listOf(guid),
                    )

                val domainRequest = request.toDomain()
                assertEquals(guid, domainRequest.id)
                assertEquals(guid, domainRequest.householdId)
                assertEquals(listOf(guid), domainRequest.tagIds)
            }
        }

    @Test
    fun `toDomain should handle null tagIds correctly`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
                    tagIds = null,
                )

            val domainRequest = request.toDomain()
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle empty tagIds correctly`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
                    tagIds = emptyList(),
                )

            val domainRequest = request.toDomain()
            assertEquals(emptyList<String>(), domainRequest.tagIds)
        }

    @Test
    fun `toDomain should handle case sensitivity for status values`() =
        runTest {
            val request =
                TaskCreationRequest(
                    id = Guid.generate(),
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "pending", // lowercase
                    householdId = Guid.generate(),
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
    fun `toDomain should generate new ID when id is not provided`() =
        runTest {
            val request =
                TaskCreationRequest(
                    title = "Test Task",
                    description = "A test task description",
                    dueDate = null,
                    status = "PENDING",
                    householdId = Guid.generate(),
                    tagIds = emptyList(),
                )

            val domainRequest = request.toDomain()

            // Should generate a new valid GUID
            assertEquals(true, Guid.isValid(domainRequest.id))
            assertEquals("Test Task", domainRequest.title)
            assertEquals("A test task description", domainRequest.description)
        }
}
