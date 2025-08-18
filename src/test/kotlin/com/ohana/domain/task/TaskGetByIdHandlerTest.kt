package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var handler: TaskGetByIdHandler

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
        handler = TaskGetByIdHandler(unitOfWork, householdMemberValidator, taskTagManager)
    }

    @Test
    fun `handle should return task when found and user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val title = "Test Task"
            val description = "Test Description"
            val dueDate = Instant.now().plusSeconds(3600)
            val status = TaskStatus.PENDING
            val createdBy = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    status = status,
                    createdBy = createdBy,
                    householdId = householdId,
                )

            val tags =
                listOf(
                    TestUtils.getTag(id = UUID.randomUUID().toString()),
                    TestUtils.getTag(id = UUID.randomUUID().toString()),
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(tags)

            val response = handler.handle(userId, taskId)

            assertEquals(taskId, response.id)
            assertEquals(title, response.title)
            assertEquals(description, response.description)
            assertEquals(dueDate, response.dueDate)
            assertEquals(status, response.status)
            assertEquals(createdBy, response.createdBy)
            assertEquals(householdId, response.householdId)
            assertEquals(tags.map { it.id }, response.tagIds)
            assertNull(response.completedAt)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskTagManager).getTaskTags(context, taskId)
            verifyNoMoreInteractions(taskRepository, taskTagManager)
        }

    @Test
    fun `handle should throw NotFoundException when task does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            whenever(taskRepository.findById(taskId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("Task not found", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
            verify(taskTagManager, never()).getTaskTags(any(), any())
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(householdMemberValidator.validate(context, householdId, userId))
                .thenThrow(
                    com.ohana.shared.exceptions
                        .AuthorizationException("User not authorized"),
                )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("User not authorized", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskTagManager, never()).getTaskTags(any(), any())
        }

    @Test
    fun `handle should work with completed task`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val completedAt = Instant.now()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    status = TaskStatus.COMPLETED,
                    completedAt = completedAt,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId)

            assertEquals(TaskStatus.COMPLETED, response.status)
            assertEquals(completedAt, response.completedAt)
        }

    @Test
    fun `handle should work with task that has null due date`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    dueDate = null,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId)

            assertNull(response.dueDate)
        }

    @Test
    fun `handle should work with task that has empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val description = ""

            val task =
                TestUtils.getTask(
                    id = taskId,
                    description = description,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId)

            assertEquals(description, response.description)
        }

    @Test
    fun `handle should work with task that has no tags`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(emptyList())

            val response = handler.handle(userId, taskId)

            assertTrue(response.tagIds.isEmpty())
        }

    @Test
    fun `handle should work with all task statuses`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            TaskStatus.values().forEach { status ->
                val task =
                    TestUtils.getTask(
                        id = taskId,
                        status = status,
                        householdId = householdId,
                        completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
                    )

                whenever(taskRepository.findById(taskId)).thenReturn(task)
                whenever(taskTagManager.getTaskTags(context, taskId)).thenReturn(emptyList())

                val response = handler.handle(userId, taskId)

                assertEquals(status, response.status)
                if (status == TaskStatus.COMPLETED) {
                    assertNotNull(response.completedAt)
                } else {
                    assertNull(response.completedAt)
                }
            }
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            whenever(taskRepository.findById(taskId)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("DB error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
            verify(taskTagManager, never()).getTaskTags(any(), any())
        }

    @Test
    fun `handle should propagate exception from task tag manager`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskTagManager.getTaskTags(context, taskId))
                .thenThrow(RuntimeException("Tag error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, taskId)
                }

            assertEquals("Tag error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskTagManager).getTaskTags(context, taskId)
        }
}
