package com.ohana.task.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.AuthorizationException
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
import java.util.UUID

class TaskGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var handler: TaskGetAllHandler

    private val userId = UUID.randomUUID().toString()
    private val householdId = UUID.randomUUID().toString()
    private val householdId2 = UUID.randomUUID().toString()

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
                        title = "Task 1",
                        description = "Description 1",
                        dueDate = Instant.now().plusSeconds(3600),
                        status = TaskStatus.pending,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Task 2",
                        description = "Description 2",
                        dueDate = Instant.now().plusSeconds(7200),
                        status = TaskStatus.in_progress,
                        createdBy = UUID.randomUUID().toString(),
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Task 3",
                        description = "Description 3",
                        dueDate = Instant.now().plusSeconds(10800),
                        status = TaskStatus.completed,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdIds(listOf(householdId))).thenReturn(tasks)

            val response = handler.handle(listOf(householdId), userId)

            verify(taskRepository).findByHouseholdIds(listOf(householdId))

            assertEquals(3, response.size)

            // Verify first task
            assertEquals(tasks[0].id, response[0].id)
            assertEquals(tasks[0].title, response[0].title)
            assertEquals(tasks[0].description, response[0].description)
            assertEquals(tasks[0].dueDate, response[0].dueDate)
            assertEquals(tasks[0].status, response[0].status)
            assertEquals(tasks[0].createdBy, response[0].createdBy)
            assertEquals(tasks[0].householdId, response[0].householdId)

            // Verify second task
            assertEquals(tasks[1].id, response[1].id)
            assertEquals(tasks[1].title, response[1].title)
            assertEquals(tasks[1].description, response[1].description)
            assertEquals(tasks[1].dueDate, response[1].dueDate)
            assertEquals(tasks[1].status, response[1].status)
            assertEquals(tasks[1].createdBy, response[1].createdBy)
            assertEquals(tasks[1].householdId, response[1].householdId)

            // Verify third task
            assertEquals(tasks[2].id, response[2].id)
            assertEquals(tasks[2].title, response[2].title)
            assertEquals(tasks[2].description, response[2].description)
            assertEquals(tasks[2].dueDate, response[2].dueDate)
            assertEquals(tasks[2].status, response[2].status)
            assertEquals(tasks[2].createdBy, response[2].createdBy)
            assertEquals(tasks[2].householdId, response[2].householdId)
        }

    @Test
    fun `handle should call validate`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            handler.handle(listOf(householdId), userId)

            verify(householdMemberValidator).validate(context, householdId, userId)
        }

    @Test
    fun `handle should return empty list when no tasks exist for household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(taskRepository.findByHouseholdIds(listOf(householdId))).thenReturn(emptyList())

            val response = handler.handle(listOf(householdId), userId)

            verify(taskRepository).findByHouseholdIds(listOf(householdId))

            assertTrue(response.isEmpty())
        }

    @Test
    fun `handle should propagate exception from validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(AuthorizationException("User is not a member of the household"))

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(listOf(householdId), userId)
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).findByHouseholdIds(any())
        }

    @Test
    fun `handle should propagate exception from task repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(taskRepository.findByHouseholdIds(listOf(householdId))).thenThrow(RuntimeException("Database connection error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(listOf(householdId), userId)
                }

            assertEquals("Database connection error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findByHouseholdIds(listOf(householdId))
        }

    @Test
    fun `handle should return tasks with all status types`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Pending Task",
                        status = TaskStatus.pending,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "In Progress Task",
                        status = TaskStatus.in_progress,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Completed Task",
                        status = TaskStatus.completed,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdIds(listOf(householdId))).thenReturn(tasks)

            val response = handler.handle(listOf(householdId), userId)

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
                        title = "My Task",
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Other User Task",
                        createdBy = UUID.randomUUID().toString(),
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Another User Task",
                        createdBy = UUID.randomUUID().toString(),
                        householdId = householdId,
                    ),
                )

            whenever(taskRepository.findByHouseholdIds(listOf(householdId))).thenReturn(tasks)

            val response = handler.handle(listOf(householdId), userId)

            assertEquals(3, response.size)
            assertEquals(tasks[0].createdBy, response[0].createdBy)
            assertEquals(tasks[1].createdBy, response[1].createdBy)
            assertEquals(tasks[2].createdBy, response[2].createdBy)
        }

    @Test
    fun `handle should return tasks from multiple households when user has access to all`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasksFromHousehold1 =
                listOf(
                    TestUtils.getTask(
                        title = "Task from Household 1",
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            val tasksFromHousehold2 =
                listOf(
                    TestUtils.getTask(
                        title = "Task from Household 2",
                        createdBy = userId,
                        householdId = householdId2,
                    ),
                )

            val allTasks = tasksFromHousehold1 + tasksFromHousehold2

            whenever(taskRepository.findByHouseholdIds(listOf(householdId, householdId2))).thenReturn(allTasks)

            val response = handler.handle(listOf(householdId, householdId2), userId)

            assertEquals(2, response.size)
            assertEquals(householdId, response[0].householdId)
            assertEquals(householdId2, response[1].householdId)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdMemberValidator).validate(context, householdId2, userId)
        }

    @Test
    fun `handle should throw AuthorizationException when user lacks access to any household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            // Don't need to mock void method success for the first household
            whenever(householdMemberValidator.validate(context, householdId2, userId)).thenThrow(
                AuthorizationException("User is not a member of household 2"),
            )

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(listOf(householdId, householdId2), userId)
                }

            assertEquals("User is not a member of household 2", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdMemberValidator).validate(context, householdId2, userId)
            verify(taskRepository, never()).findByHouseholdIds(any())
        }

    @Test
    fun `handle should return empty list when no household IDs provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val response = handler.handle(emptyList(), userId)

            assertTrue(response.isEmpty())
            verify(taskRepository).findByHouseholdIds(emptyList())
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }
}
