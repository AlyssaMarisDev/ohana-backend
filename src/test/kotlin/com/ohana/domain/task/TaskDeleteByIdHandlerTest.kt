package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.AuthorizationException
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class TaskDeleteByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var handler: TaskDeleteByIdHandler
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
        handler = TaskDeleteByIdHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should delete task when task exists and user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = userId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(taskId, userId)

            assertTrue(result)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).deleteById(taskId)
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
                    handler.handle(taskId, userId)
                }

            assertEquals("Task not found", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
            verify(taskRepository, never()).deleteById(any())
        }

    @Test
    fun `handle should throw AuthorizationException when user is not a member of the household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = UUID.randomUUID().toString(), // Different user
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(AuthorizationException("User is not a member of the household"))

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(taskId, userId)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).deleteById(any())
        }

    @Test
    fun `handle should propagate exception from task repository findById`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()

            whenever(taskRepository.findById(taskId)).thenThrow(RuntimeException("Database connection error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(taskId, userId)
                }

            assertEquals("Database connection error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator, never()).validate(any(), any(), any())
            verify(taskRepository, never()).deleteById(any())
        }

    @Test
    fun `handle should propagate exception from task repository deleteById`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = userId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskRepository.deleteById(taskId)).thenThrow(RuntimeException("Delete operation failed"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(taskId, userId)
                }

            assertEquals("Delete operation failed", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).deleteById(taskId)
        }

    @Test
    fun `handle should propagate exception from household member validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = userId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(IllegalStateException("Validator internal error"))

            val ex =
                assertThrows<IllegalStateException> {
                    handler.handle(taskId, userId)
                }

            assertEquals("Validator internal error", ex.message)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).deleteById(any())
        }

    @Test
    fun `handle should return true when task is successfully deleted`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = userId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskRepository.deleteById(taskId)).thenReturn(true)

            val result = handler.handle(taskId, userId)

            assertTrue(result)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).deleteById(taskId)
        }

    @Test
    fun `handle should return false when task deletion returns false`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val taskId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                    createdBy = userId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(taskRepository.deleteById(taskId)).thenReturn(false)

            val result = handler.handle(taskId, userId)

            assertFalse(result)
            verify(taskRepository).findById(taskId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).deleteById(taskId)
        }
}
