package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
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

class TaskCreationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var handler: TaskCreationHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdRepository = mock()
        householdMemberValidator = mock()
        taskTagManager = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = TaskCreationHandler(unitOfWork, householdMemberValidator, taskTagManager)
    }

    @Test
    fun `handle should create task and return response when valid request provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val title = "Test Task"
            val description = "Test Description"
            val dueDate = Instant.now().plusSeconds(3600)
            val status = TaskStatus.PENDING
            val tagIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())

            val task =
                TestUtils.getTask(
                    id = taskId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    status = status,
                    createdBy = userId,
                    householdId = householdId,
                )

            val tags = tagIds.map { TestUtils.getTag(id = it) }

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, tagIds)).thenReturn(tags)

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    status = status,
                    householdId = householdId,
                    tagIds = tagIds,
                )

            val response = handler.handle(userId, request)

            assertEquals(taskId, response.id)
            assertEquals(title, response.title)
            assertEquals(description, response.description)
            assertEquals(dueDate, response.dueDate)
            assertEquals(status, response.status)
            assertEquals(userId, response.createdBy)
            assertEquals(householdId, response.householdId)
            assertEquals(tagIds, response.tagIds)
            assertNull(response.completedAt)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).create(
                argThat { createdTask ->
                    createdTask.id == taskId &&
                        createdTask.title == title &&
                        createdTask.description == description &&
                        createdTask.dueDate == dueDate &&
                        createdTask.status == status &&
                        createdTask.createdBy == userId &&
                        createdTask.householdId == householdId &&
                        createdTask.completedAt == null
                },
            )
            verify(taskTagManager).assignTagsToTask(context, taskId, tagIds)
            verifyNoMoreInteractions(taskRepository, taskTagManager)
        }

    @Test
    fun `handle should set completedAt when task is created with COMPLETED status`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val status = TaskStatus.COMPLETED

            val task =
                TestUtils.getTask(
                    id = taskId,
                    status = status,
                    createdBy = userId,
                    householdId = householdId,
                    completedAt = Instant.now(),
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, emptyList())).thenReturn(emptyList())

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = status,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val response = handler.handle(userId, request)

            assertEquals(status, response.status)
            assertNotNull(response.completedAt)

            verify(taskRepository).create(
                argThat { createdTask ->
                    createdTask.status == TaskStatus.COMPLETED &&
                        createdTask.completedAt != null
                },
            )
        }

    @Test
    fun `handle should work with empty description`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val description = ""

            val task =
                TestUtils.getTask(
                    id = taskId,
                    description = description,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, emptyList())).thenReturn(emptyList())

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = description,
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val response = handler.handle(userId, request)

            assertEquals(description, response.description)
        }

    @Test
    fun `handle should work with null due date`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    dueDate = null,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, emptyList())).thenReturn(emptyList())

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val response = handler.handle(userId, request)

            assertNull(response.dueDate)
        }

    @Test
    fun `handle should work with empty tag list`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, emptyList())).thenReturn(emptyList())

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val response = handler.handle(userId, request)

            assertTrue(response.tagIds.isEmpty())
        }

    @Test
    fun `handle should work with all task statuses`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            TaskStatus.values().forEach { status ->
                val task =
                    TestUtils.getTask(
                        id = taskId,
                        status = status,
                        createdBy = userId,
                        householdId = householdId,
                        completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
                    )

                whenever(taskRepository.create(any())).thenReturn(task)
                whenever(taskTagManager.assignTagsToTask(context, taskId, emptyList())).thenReturn(emptyList())

                val request =
                    TaskCreationHandler.Request(
                        id = taskId,
                        title = "Test Task",
                        description = "Test Description",
                        dueDate = null,
                        status = status,
                        householdId = householdId,
                        tagIds = emptyList(),
                    )

                val response = handler.handle(userId, request)

                assertEquals(status, response.status)
                if (status == TaskStatus.COMPLETED) {
                    assertNotNull(response.completedAt)
                } else {
                    assertNull(response.completedAt)
                }
            }
        }

    @Test
    fun `handle should propagate exception from household member validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            whenever(householdMemberValidator.validate(context, householdId, userId))
                .thenThrow(
                    com.ohana.shared.exceptions
                        .AuthorizationException("User not authorized"),
                )

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, request)
                }

            assertEquals("User not authorized", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).create(any())
            verify(taskTagManager, never()).assignTagsToTask(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from task repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()

            whenever(taskRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("DB error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).create(any())
            verify(taskTagManager, never()).assignTagsToTask(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from task tag manager`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            val tagIds = listOf(UUID.randomUUID().toString())

            val task =
                TestUtils.getTask(
                    id = taskId,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, taskId, tagIds))
                .thenThrow(RuntimeException("Tag assignment error"))

            val request =
                TaskCreationHandler.Request(
                    id = taskId,
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = null,
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = tagIds,
                )

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }

            assertEquals("Tag assignment error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).create(any())
            verify(taskTagManager).assignTagsToTask(context, taskId, tagIds)
        }
}
