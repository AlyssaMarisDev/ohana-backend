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
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var handler: TaskUpdateByIdHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        validator = mock()
        taskTagManager = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { households } doReturn mock()
            }
        unitOfWork = mock()
        handler = TaskUpdateByIdHandler(unitOfWork, validator, taskTagManager)
    }

    @Test
    fun `handle should update task and tags`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val existing = TestUtils.getTask()
            whenever(taskRepository.findById(existing.id)).thenReturn(existing)
            whenever(validator.validate(context, existing.householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(taskRepository.update(any())).thenAnswer { it.getArgument(0) }
            whenever(taskTagManager.assignTagsToTask(eq(context), eq(existing.id), any())).thenReturn(emptyList())

            val req =
                TaskUpdateByIdHandler.Request(
                    title = "T",
                    description = "D",
                    dueDate = Instant.now(),
                    status = TaskStatus.COMPLETED,
                    tagIds = listOf(UUID.randomUUID().toString()),
                )
            val res = handler.handle(userId, existing.id, req)
            assertEquals("T", res.title)
            assertEquals(TaskStatus.COMPLETED, res.status)
        }

    @Test
    fun `handle should set completedAt when status changes to completed`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val existing = TestUtils.getTask(status = TaskStatus.PENDING)
            whenever(taskRepository.findById(existing.id)).thenReturn(existing)
            whenever(validator.validate(context, existing.householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(taskRepository.update(any())).thenAnswer { it.getArgument(0) }
            whenever(taskTagManager.assignTagsToTask(eq(context), eq(existing.id), any())).thenReturn(emptyList())

            val req =
                TaskUpdateByIdHandler.Request(
                    existing.title,
                    existing.description,
                    existing.dueDate,
                    TaskStatus.COMPLETED,
                    emptyList(),
                )
            val res = handler.handle(userId, existing.id, req)
            // completedAt should be set recently
            requireNotNull(res.completedAt)
        }

    @Test
    fun `handle should throw NotFound when task missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            whenever(taskRepository.findById(id)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, id, TaskUpdateByIdHandler.Request("t", "d", null, TaskStatus.PENDING, emptyList()))
                }
            assertEquals("Task not found", ex.message)
        }
}
