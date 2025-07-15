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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

class TaskGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: TaskGetAllHandler

    private val userId = "user-1"
    private val householdId = "household-1"

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdMemberValidator = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
            }
        unitOfWork = mock()
        handler = TaskGetAllHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should return all tasks for household when user is valid member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasks =
                listOf(
                    TestUtils.getTask(
                        id = "task-1",
                        title = "Task 1",
                        description = "Description 1",
                        dueDate = Instant.now().plusSeconds(3600),
                        status = TaskStatus.pending,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-2",
                        title = "Task 2",
                        description = "Description 2",
                        dueDate = Instant.now().plusSeconds(7200),
                        status = TaskStatus.in_progress,
                        createdBy = "user-2",
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-3",
                        title = "Task 3",
                        description = "Description 3",
                        dueDate = Instant.now().plusSeconds(10800),
                        status = TaskStatus.completed,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)

            val response = handler.handle(householdId, userId)

            verify(taskRepository).findByHouseholdId(householdId)

            assertEquals(3, response.size)

            // Verify first task
            assertEquals("task-1", response[0].id)
            assertEquals("Task 1", response[0].title)
            assertEquals("Description 1", response[0].description)
            assertEquals(tasks[0].dueDate, response[0].dueDate)
            assertEquals(TaskStatus.pending, response[0].status)
            assertEquals(userId, response[0].createdBy)
            assertEquals(householdId, response[0].householdId)

            // Verify second task
            assertEquals("task-2", response[1].id)
            assertEquals("Task 2", response[1].title)
            assertEquals("Description 2", response[1].description)
            assertEquals(tasks[1].dueDate, response[1].dueDate)
            assertEquals(TaskStatus.in_progress, response[1].status)
            assertEquals("user-2", response[1].createdBy)
            assertEquals(householdId, response[1].householdId)

            // Verify third task
            assertEquals("task-3", response[2].id)
            assertEquals("Task 3", response[2].title)
            assertEquals("Description 3", response[2].description)
            assertEquals(tasks[2].dueDate, response[2].dueDate)
            assertEquals(TaskStatus.completed, response[2].status)
            assertEquals(userId, response[2].createdBy)
            assertEquals(householdId, response[2].householdId)
        }

    @Test
    fun `handle should call validate`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            handler.handle(householdId, userId)

            verify(householdMemberValidator).validate(context, householdId, userId)
        }

    @Test
    fun `handle should return empty list when no tasks exist for household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(emptyList())

            val response = handler.handle(householdId, userId)

            verify(taskRepository).findByHouseholdId(householdId)

            assertTrue(response.isEmpty())
        }

    @Test
    fun `handle should throw AuthorizationException when user is not a member of household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(AuthorizationException("User is not a member of the household"))

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(householdId, userId)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).findByHouseholdId(any())
        }

    @Test
    fun `handle should throw NotFoundException when household does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(NotFoundException("Household not found"))

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(householdId, userId)
                }

            assertEquals("Household not found", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).findByHouseholdId(any())
        }

    @Test
    fun `handle should propagate exception from task repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(taskRepository.findByHouseholdId(householdId)).thenThrow(RuntimeException("Database connection error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(householdId, userId)
                }

            assertEquals("Database connection error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findByHouseholdId(householdId)
        }

    @Test
    fun `handle should return tasks with all status types`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasks =
                listOf(
                    TestUtils.getTask(
                        id = "task-1",
                        title = "Pending Task",
                        status = TaskStatus.pending,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-2",
                        title = "In Progress Task",
                        status = TaskStatus.in_progress,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-3",
                        title = "Completed Task",
                        status = TaskStatus.completed,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)

            val response = handler.handle(householdId, userId)

            assertEquals(3, response.size)
            assertEquals(TaskStatus.pending, response[0].status)
            assertEquals(TaskStatus.in_progress, response[1].status)
            assertEquals(TaskStatus.completed, response[2].status)
        }

    @Test
    fun `handle should return tasks created by different users`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasks =
                listOf(
                    TestUtils.getTask(
                        id = "task-1",
                        title = "My Task",
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-2",
                        title = "Other User Task",
                        createdBy = "user-2",
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        id = "task-3",
                        title = "Another User Task",
                        createdBy = "user-3",
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdId(householdId)).thenReturn(tasks)

            val response = handler.handle(householdId, userId)

            assertEquals(3, response.size)
            assertEquals(userId, response[0].createdBy)
            assertEquals("user-2", response[1].createdBy)
            assertEquals("user-3", response[2].createdBy)
        }
}
