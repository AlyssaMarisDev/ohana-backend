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
import java.util.UUID

class TaskDeleteByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var handler: TaskDeleteByIdHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        validator = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { households } doReturn mock()
            }
        unitOfWork = mock()
        handler = TaskDeleteByIdHandler(unitOfWork, validator)
    }

    @Test
    fun `handle should delete when authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val task = TestUtils.getTask()
            whenever(taskRepository.findById(task.id)).thenReturn(task)
            whenever(validator.validate(context, task.householdId, userId)).thenReturn(UUID.randomUUID().toString())
            whenever(taskRepository.deleteById(task.id)).thenReturn(true)

            val result = handler.handle(userId, task.id)
            assertEquals(true, result)
            verify(taskRepository).deleteById(task.id)
        }

    @Test
    fun `handle should throw NotFound when missing`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val taskId = UUID.randomUUID().toString()
            whenever(taskRepository.findById(taskId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> { handler.handle(userId, taskId) }
            assertEquals("Task not found", ex.message)
        }
}
