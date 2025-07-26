package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.*
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException
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
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var handler: TaskUpdateByIdHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdMemberValidator = mock()
        taskTagManager = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
            }
        unitOfWork = mock()
        handler = TaskUpdateByIdHandler(unitOfWork, householdMemberValidator, taskTagManager)
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
                    tagIds = emptyList(),
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
            whenever(taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId, request)

            assertEquals(taskId, response.id)
            assertEquals("Updated Title", response.title)
            assertEquals("Updated Description", response.description)
            assertEquals(request.dueDate, response.dueDate)
            assertEquals(TaskStatus.IN_PROGRESS, response.status)
            assertEquals(userId, response.createdBy)
            assertEquals(householdId, response.householdId)
            assertEquals(0, response.tagIds.size)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).update(
                argThat { task ->
                    task.id == taskId &&
                        task.title == "Updated Title" &&
                        task.description == "Updated Description" &&
                        task.dueDate == request.dueDate &&
                        task.status == TaskStatus.IN_PROGRESS &&
                        task.completedAt == null
                },
            )
            verify(taskTagManager).assignTagsToTask(context, updatedTask.id, request.tagIds)
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
                    tagIds = emptyList(),
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
                    tagIds = emptyList(),
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
                    tagIds = emptyList(),
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
                    tagIds = emptyList(),
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
                    tagIds = emptyList(),
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
            whenever(taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId, request)

            assertEquals(originalCreatorId, response.createdBy)
            assertEquals(householdId, response.householdId)
            verify(taskRepository).update(
                argThat { task ->
                    task.createdBy == originalCreatorId && task.householdId == householdId
                },
            )
        }

    @Test
    fun `handle should set completed_at when task is marked as completed`() =
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
                    status = TaskStatus.COMPLETED,
                    tagIds = emptyList(),
                )

            val updatedTask =
                existingTask.copy(
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                    completedAt = Instant.now(),
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenReturn(updatedTask)
            whenever(taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId, request)

            assertEquals(TaskStatus.COMPLETED, response.status)
            assertEquals(updatedTask.completedAt, response.completedAt)

            verify(taskRepository).update(
                argThat { task ->
                    task.status == TaskStatus.COMPLETED && task.completedAt != null
                },
            )
        }

    @Test
    fun `handle should clear completed_at when task is unmarked as completed`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val completedAt = Instant.now().minusSeconds(3600)

            val existingTask =
                TestUtils.getTask(
                    id = taskId,
                    title = "Original Title",
                    description = "Original Description",
                    dueDate = Instant.now().plusSeconds(3600),
                    status = TaskStatus.COMPLETED,
                    completedAt = completedAt,
                    createdBy = userId,
                    householdId = householdId,
                )

            val request =
                TaskUpdateByIdHandler.Request(
                    title = "Updated Title",
                    description = "Updated Description",
                    dueDate = Instant.now().plusSeconds(7200),
                    status = TaskStatus.IN_PROGRESS,
                    tagIds = emptyList(),
                )

            val updatedTask =
                existingTask.copy(
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                    completedAt = null,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(existingTask)
            whenever(taskRepository.update(any())).thenReturn(updatedTask)
            whenever(taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId, request)

            assertEquals(TaskStatus.IN_PROGRESS, response.status)
            assertEquals(null, response.completedAt)

            verify(taskRepository).update(
                argThat { task ->
                    task.status == TaskStatus.IN_PROGRESS && task.completedAt == null
                },
            )
        }
}
