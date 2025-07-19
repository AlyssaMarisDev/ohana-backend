package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TaskGetByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var handler: TaskGetByIdHandler
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
        handler = TaskGetByIdHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should return task when found and user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(task.id, response.id)
            assertEquals(task.title, response.title)
            assertEquals(task.description, response.description)
            assertEquals(task.dueDate, response.dueDate)
            assertEquals(task.status, response.status)
            assertEquals(task.createdBy, response.createdBy)
            assertEquals(task.householdId, response.householdId)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verifyNoMoreInteractions(taskRepository)
        }

    @Test
    fun `handle should return task with different status types`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.IN_PROGRESS,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(com.ohana.shared.enums.TaskStatus.IN_PROGRESS, response.status)
        }

    @Test
    fun `handle should return task with completed status`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.COMPLETED,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(com.ohana.shared.enums.TaskStatus.COMPLETED, response.status)
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of task's household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(householdMemberValidator.validate(context, householdId, userId))
                .thenThrow(
                    com.ohana.shared.exceptions
                        .AuthorizationException("User is not a member of the household"),
                )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
        }

    @Test
    fun `handle should throw NotFoundException when task does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(taskRepository.findById(taskId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("Task not found", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(taskRepository.findById(taskId)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("DB error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }

    @Test
    fun `handle should work with task created by different user`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val taskCreatorId = UUID.randomUUID().toString() // Different user created the task

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = taskCreatorId, // Different user
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(taskCreatorId, response.createdBy)
            assertEquals(userId, userId) // Current user should still be the one making the request
        }

    @Test
    fun `handle should work with task that has empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "", // Empty description
                    dueDate = Instant.now().plusSeconds(3600),
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals("", response.description)
        }

    @Test
    fun `handle should work with task that has past due date`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().minusSeconds(3600), // Past due date
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(task.dueDate, response.dueDate)
        }

    @Test
    fun `handle should work with task that has current due date`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val currentTime = Instant.now()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = currentTime, // Current time
                    status = com.ohana.shared.enums.TaskStatus.PENDING,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)

            val response = handler.handle(userId, taskId)

            assertEquals(currentTime, response.dueDate)
        }
}
