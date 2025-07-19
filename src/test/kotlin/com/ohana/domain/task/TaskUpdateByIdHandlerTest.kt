package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TaskUpdateByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var handler: TaskUpdateByIdHandler
    private lateinit var householdMemberValidator: HouseholdMemberValidator
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
    fun `handle should update task when validation passes`() =
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

            val expectedUpdatedTask =
                existingTask.copy(
                    title = request.title,
                    description = request.description!!,
                    dueDate = request.dueDate,
                    status = request.status!!,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenReturn(expectedUpdatedTask)

            val response = handler.handle(taskId, householdId, userId, request)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).update(expectedUpdatedTask)

            assertEquals(expectedUpdatedTask.id, response.id)
            assertEquals(expectedUpdatedTask.title, response.title)
            assertEquals(expectedUpdatedTask.description, response.description)
            assertEquals(expectedUpdatedTask.dueDate, response.dueDate)
            assertEquals(expectedUpdatedTask.status, response.status)
            assertEquals(expectedUpdatedTask.createdBy, response.createdBy)
            assertEquals(expectedUpdatedTask.householdId, response.householdId)
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

            whenever(householdMemberValidator.validate(context, householdId, userId))
                .thenThrow(AuthorizationException("User is not a member of the household"))

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(taskId, householdId, userId, request)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).findById(any())
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
                    handler.handle(taskId, householdId, userId, request)
                }

            assertEquals("Task not found", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository, never()).update(any())
        }

    @Test
    fun `handle should throw NotFoundException when task exists but not in household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val otherHouseholdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = otherHouseholdId, // Different household
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(taskId, householdId, userId, request)
                }

            assertEquals("Task not found in this household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository, never()).update(any())
        }

    @Test
    fun `handle should propagate exception from repository`() =
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
            whenever(taskRepository.update(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(taskId, householdId, userId, request)
                }

            assertEquals("DB error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).update(any())
        }

    @Test
    fun `Request validation should pass with valid data`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        assertEquals(0, violations.size)
    }

    @Test
    fun `Request validation should fail with empty title`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "",
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val messages = violations.map { it.propertyPath.toString() to it.message }
        assertEquals(2, violations.size)
        assertTrue(messages.contains("title" to "Title is required"))
        assertTrue(messages.contains("title" to "Title must be between 1 and 255 characters long"))
    }

    @Test
    fun `Request validation should fail with title too short`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "", // Empty string is less than 1 character
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val messages = violations.map { it.propertyPath.toString() to it.message }
        assertEquals(2, violations.size)
        assertTrue(messages.contains("title" to "Title is required"))
        assertTrue(messages.contains("title" to "Title must be between 1 and 255 characters long"))
    }

    @Test
    fun `Request validation should fail with title too long`() {
        val longTitle = "a".repeat(256) // 256 characters
        val request =
            TaskUpdateByIdHandler.Request(
                title = longTitle,
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val messages = violations.map { it.propertyPath.toString() to it.message }
        assertEquals(1, violations.size)
        assertTrue(messages.contains("title" to "Title must be between 1 and 255 characters long"))
    }

    @Test
    fun `Request validation should fail with description too long`() {
        val longDescription = "a".repeat(1001) // 1001 characters
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = longDescription,
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val messages = violations.map { it.propertyPath.toString() to it.message }
        assertEquals(1, violations.size)
        assertTrue(messages.contains("description" to "Description must be at most 1000 characters long"))
    }

    @Test
    fun `Request validation should fail with past due date`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = "Valid description",
                dueDate = Instant.now().minusSeconds(3600), // Past date
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val messages = violations.map { it.propertyPath.toString() to it.message }
        assertEquals(1, violations.size)
        assertTrue(messages.contains("dueDate" to "Due date cannot be in the past"))
    }

    @Test
    fun `Request validation should pass with future due date`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600), // Future date
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        assertEquals(0, violations.size)
    }

    @Test
    fun `Request validation should handle multiple validation errors`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "", // Empty title
                description = "a".repeat(1001), // Too long description
                dueDate = Instant.now().minusSeconds(3600), // Past date
                status = TaskStatus.PENDING,
            )
        val violations = validator.validate(request)
        val fields = violations.map { it.propertyPath.toString() }.toSet()
        assertEquals(3, fields.size)
        assertTrue(fields.contains("title"))
        assertTrue(fields.contains("description"))
        assertTrue(fields.contains("dueDate"))
    }
}
