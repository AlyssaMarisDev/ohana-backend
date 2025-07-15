package com.ohana.task.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.AuthorizationException
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.TaskRepository
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
                    status = TaskStatus.pending,
                    createdBy = userId,
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.in_progress,
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
                    status = TaskStatus.in_progress,
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
                    status = TaskStatus.in_progress,
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
                    status = TaskStatus.pending,
                    createdBy = userId,
                    householdId = otherHouseholdId, // Different household
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.in_progress,
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
                    status = TaskStatus.pending,
                    createdBy = userId,
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.in_progress,
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
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(0, errors.size)
    }

    @Test
    fun `Request validation should fail with empty title`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "",
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(2, errors.size)
        assertEquals("title", errors[0].field)
        assertEquals("Title is required", errors[0].message)
        assertEquals("title", errors[1].field)
        assertEquals("Title must be at least 1 character long", errors[1].message)
    }

    @Test
    fun `Request validation should fail with title too short`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "", // Empty string is less than 1 character
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(2, errors.size)
        assertEquals("title", errors[0].field)
        assertEquals("Title is required", errors[0].message)
        assertEquals("title", errors[1].field)
        assertEquals("Title must be at least 1 character long", errors[1].message)
    }

    @Test
    fun `Request validation should fail with title too long`() {
        val longTitle = "a".repeat(256) // 256 characters
        val request =
            TaskUpdateByIdHandler.Request(
                title = longTitle,
                description = "Valid description",
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(1, errors.size)
        assertEquals("title", errors[0].field)
        assertEquals("Title must be at most 255 characters long", errors[0].message)
    }

    @Test
    fun `Request validation should fail with description too long`() {
        val longDescription = "a".repeat(1001) // 1001 characters
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = longDescription,
                dueDate = Instant.now().plusSeconds(3600),
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(1, errors.size)
        assertEquals("description", errors[0].field)
        assertEquals("Description must be at most 1000 characters long", errors[0].message)
    }

    @Test
    fun `Request validation should fail with past due date`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = "Valid description",
                dueDate = Instant.now().minusSeconds(3600), // Past date
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(1, errors.size)
        assertEquals("dueDate", errors[0].field)
        assertEquals("Due date cannot be in the past", errors[0].message)
    }

    @Test
    fun `Request validation should pass with current due date`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "Valid Title",
                description = "Valid description",
                dueDate = Instant.now(), // Current time
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(0, errors.size)
    }

    @Test
    fun `Request validation should handle multiple validation errors`() {
        val request =
            TaskUpdateByIdHandler.Request(
                title = "", // Empty title
                description = "a".repeat(1001), // Too long description
                dueDate = Instant.now().minusSeconds(3600), // Past date
                status = TaskStatus.pending,
            )

        val errors = request.validate()

        assertEquals(4, errors.size)

        val errorFields = errors.map { it.field }.toSet()
        assertEquals(setOf("title", "description", "dueDate"), errorFields)
    }
}
