package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class TaskGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: com.ohana.data.task.TaskRepository
    private lateinit var householdRepository: com.ohana.data.household.HouseholdRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var handler: TaskGetAllHandler

    @BeforeEach
    fun setUp() {
        unitOfWork = mock()
        context = mock()
        taskRepository = mock()
        householdRepository = mock()
        validator = mock()
        taskTagManager = mock()
        tagPermissionManager = mock()
        handler = TaskGetAllHandler(unitOfWork, validator, taskTagManager, tagPermissionManager)
    }

    @Test
    fun `handle should return filtered tasks for household`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val request =
                TaskGetAllHandler.Request(
                    householdId = householdId,
                    status = TaskStatus.PENDING,
                    tagIds = listOf("tag1", "tag2"),
                )

            val tasks =
                listOf(
                    TestUtils.getTask(id = "task1", householdId = householdId, status = TaskStatus.PENDING),
                    TestUtils.getTask(id = "task2", householdId = householdId, status = TaskStatus.PENDING),
                    TestUtils.getTask(id = "task3", householdId = householdId, status = TaskStatus.COMPLETED),
                )

            val permittedTaskIds = listOf("task1", "task2")
            val taskTagsMap =
                mapOf(
                    "task1" to listOf(TestUtils.getTag(id = "tag1")),
                    "task2" to listOf(TestUtils.getTag(id = "tag2")),
                )

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(context.tasks).thenReturn(taskRepository)
            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)
            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagPermissionManager.filterTasksByTagPermissions(context, householdMemberId, listOf("task1", "task2")))
                .thenReturn(permittedTaskIds)
            whenever(taskTagManager.getTasksTags(context, permittedTaskIds)).thenReturn(taskTagsMap)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(2, response.tasks.size)
            assertEquals("task1", response.tasks[0].id)
            assertEquals("task2", response.tasks[1].id)

            verify(validator).validate(context, householdId, userId)
            verify(tagPermissionManager).filterTasksByTagPermissions(context, householdMemberId, listOf("task1", "task2"))
            verify(taskTagManager).getTasksTags(context, permittedTaskIds)
        }

    @Test
    fun `handle should return all tasks when no filters applied`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val request = TaskGetAllHandler.Request(householdId = householdId)

            val tasks =
                listOf(
                    TestUtils.getTask(id = "task1", householdId = householdId),
                    TestUtils.getTask(id = "task2", householdId = householdId),
                    TestUtils.getTask(id = "task3", householdId = householdId),
                )

            val permittedTaskIds = listOf("task1", "task2", "task3")

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(context.tasks).thenReturn(taskRepository)
            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)
            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagPermissionManager.filterTasksByTagPermissions(context, householdMemberId, listOf("task1", "task2", "task3")))
                .thenReturn(permittedTaskIds)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(3, response.tasks.size)
            assertEquals("task1", response.tasks[0].id)
            assertEquals("task2", response.tasks[1].id)
            assertEquals("task3", response.tasks[2].id)
        }

    @Test
    fun `handle should return empty list when no permitted tasks`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val request = TaskGetAllHandler.Request(householdId = householdId)

            val tasks =
                listOf(
                    TestUtils.getTask(id = "task1", householdId = householdId),
                    TestUtils.getTask(id = "task2", householdId = householdId),
                )

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(context.tasks).thenReturn(taskRepository)
            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)
            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagPermissionManager.filterTasksByTagPermissions(context, householdMemberId, listOf("task1", "task2")))
                .thenReturn(emptyList())

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(0, response.tasks.size)
        }

    @Test
    fun `handle should filter by status correctly`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val request =
                TaskGetAllHandler.Request(
                    householdId = householdId,
                    status = TaskStatus.COMPLETED,
                )

            val tasks =
                listOf(
                    TestUtils.getTask(id = "task1", householdId = householdId, status = TaskStatus.PENDING),
                    TestUtils.getTask(id = "task2", householdId = householdId, status = TaskStatus.COMPLETED),
                    TestUtils.getTask(id = "task3", householdId = householdId, status = TaskStatus.COMPLETED),
                )

            val permittedTaskIds = listOf("task2", "task3")

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(context.tasks).thenReturn(taskRepository)
            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)
            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagPermissionManager.filterTasksByTagPermissions(context, householdMemberId, listOf("task2", "task3")))
                .thenReturn(permittedTaskIds)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(2, response.tasks.size)
            assertEquals("task2", response.tasks[0].id)
            assertEquals("task3", response.tasks[1].id)
        }
}
