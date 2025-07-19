package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TaskDeleteByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: TaskDeleteByIdHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdMemberValidator = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
            }
        unitOfWork = mock()
        handler = TaskDeleteByIdHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should delete task successfully when validation passes`() =
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
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(userId, taskId)

            assertTrue(result)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).deleteById(taskId)
            verifyNoMoreInteractions(taskRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user is not member of household`() =
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
            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(
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
            verify(taskRepository, never()).deleteById(any())
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
            verify(taskRepository, never()).deleteById(any())
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
            verify(taskRepository, never()).deleteById(any())
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
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(userId, taskId)

            assertTrue(result)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).deleteById(taskId)
        }

    @Test
    fun `handle should work with task that has different status`() =
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
                    status = com.ohana.shared.enums.TaskStatus.COMPLETED, // Different status
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(userId, taskId)

            assertTrue(result)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).deleteById(taskId)
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
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(userId, taskId)

            assertTrue(result)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).deleteById(taskId)
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
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(userId, taskId)

            assertTrue(result)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findById(taskId)
            verify(taskRepository).deleteById(taskId)
        }
}
