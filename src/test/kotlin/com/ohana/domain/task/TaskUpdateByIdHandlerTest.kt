package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.*
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID
import kotlin.test.assertTrue

class TaskUpdateByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: TaskUpdateByIdHandler
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdMemberValidator = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
            }
        unitOfWork = mock()
        handler = TaskUpdateByIdHandler(unitOfWork, householdMemberValidator)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should update task successfully when validation passes`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            val updatedTask =
                existingTask.copy(
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenReturn(updatedTask)

            val response = handler.handle(userId, taskId, request)

            assertEquals(taskId, response.id)
            assertEquals("Updated Title", response.title)
            assertEquals("Updated Description", response.description)
            assertEquals(request.dueDate, response.dueDate)
            assertEquals(TaskStatus.IN_PROGRESS, response.status)
            assertEquals(userId, response.createdBy)
            assertEquals(householdId, response.householdId)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).update(
                argThat { task ->
                    task.id == taskId &&
                        task.title == "Updated Title" &&
                        task.description == "Updated Description" &&
                        task.dueDate == request.dueDate &&
                        task.status == TaskStatus.IN_PROGRESS
                },
            )
            verifyNoMoreInteractions(taskRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member of the household"),
            )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, taskId, request)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).update(any())
        }

    @Test
    fun `handle should throw NotFoundException when task does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, taskId, request)
                }

            assertEquals("Task not found", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
            verify(taskRepository, never()).update(any())
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of task's household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member of the household"),
            )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, taskId, request)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).update(any())
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val existingTask = TestUtils.getTask(id = taskId, householdId = householdId)
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, taskId, request)
                }

            assertEquals("DB error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).update(any())
        }

    @Test
    fun `handle should throw ValidationException when title is empty`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("title" to "Title is required"))
        }

    @Test
    fun `handle should throw ValidationException when title is too long`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "A".repeat(256), // 256 characters, exceeds 255 limit
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("title" to "Title must be between 1 and 255 characters long"))
        }

    @Test
    fun `handle should accept valid title length`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "A".repeat(255), // Maximum length
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should accept minimum valid title length`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "A", // Minimum length
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should throw ValidationException when description is too long`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Valid Title",
                    description = "A".repeat(1001), // 1001 characters, exceeds 1000 limit
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("description" to "Description must be at most 1000 characters long"))
        }

    @Test
    fun `handle should accept valid description length`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Valid Title",
                    description = "A".repeat(1000), // Maximum length
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should accept empty description`() =
        runTest {
            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Valid Title",
                    description = "", // Empty description
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should preserve createdBy and householdId from existing task`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val originalCreatorId = UUID.randomUUID().toString() // Different user created the task

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.PENDING,
                    createdBy = originalCreatorId, // Different user
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            val updatedTask =
                existingTask.copy(
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenReturn(updatedTask)

            val response = handler.handle(userId, taskId, request)

            assertEquals(originalCreatorId, response.createdBy)
            assertEquals(householdId, response.householdId)
            verify(taskRepository).update(
                argThat { task ->
                    task.createdBy == originalCreatorId && task.householdId == householdId
                },
            )
        }
}
