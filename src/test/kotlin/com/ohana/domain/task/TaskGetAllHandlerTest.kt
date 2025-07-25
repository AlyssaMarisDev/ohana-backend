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
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var handler: TaskGetAllHandler

    private val userId = UUID.randomUUID().toString()
    private val householdId = UUID.randomUUID().toString()
    private val householdId2 = UUID.randomUUID().toString()

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
        handler = TaskGetAllHandler(unitOfWork, householdMemberValidator, taskTagManager)
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
                        status = TaskStatus.PENDING,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Task 2",
                        description = "Description 2",
                        dueDate = Instant.now().plusSeconds(7200),
                        status = TaskStatus.IN_PROGRESS,
                        createdBy = UUID.randomUUID().toString(),
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Task 3",
                        description = "Description 3",
                        dueDate = Instant.now().plusSeconds(10800),
                        status = TaskStatus.COMPLETED,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)
            whenever(taskTagManager.getTasksTags(context, tasks.map { it.id })).thenReturn(emptyMap())

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = listOf(householdId),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )

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

            handler.handle(
                userId,
                TaskGetAllHandler.Request(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            )

            verify(householdMemberValidator).validate(context, householdId, userId)
        }

    @Test
    fun `handle should return empty list when no tasks exist for household`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(emptyList())
            whenever(taskTagManager.getTasksTags(context, emptyList())).thenReturn(emptyMap())

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = listOf(householdId),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )

            assertTrue(response.isEmpty())
        }

    @Test
    fun `handle should propagate exception from validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
            ).thenThrow(
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member of the household"),
            )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(
                        userId,
                        TaskGetAllHandler.Request(
                            householdIds = listOf(householdId),
                            dueDateFrom = null,
                            dueDateTo = null,
                            completedDateFrom = null,
                            completedDateTo = null,
                        ),
                    )
                }

            assertEquals("User is not a member of the household", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository, never()).findByHouseholdIdsWithDateFilters(any(), any(), any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from task repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenThrow(RuntimeException("Database connection error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(
                        userId,
                        TaskGetAllHandler.Request(
                            householdIds = listOf(householdId),
                            dueDateFrom = null,
                            dueDateTo = null,
                            completedDateFrom = null,
                            completedDateTo = null,
                        ),
                    )
                }

            assertEquals("Database connection error", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )
        }

    @Test
    fun `handle should return tasks with all status types`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Pending Task",
                        status = TaskStatus.PENDING,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "In Progress Task",
                        status = TaskStatus.IN_PROGRESS,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Completed Task",
                        status = TaskStatus.COMPLETED,
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = listOf(householdId),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            assertEquals(3, response.size)
            assertEquals(TaskStatus.PENDING, response[0].status)
            assertEquals(TaskStatus.IN_PROGRESS, response[1].status)
            assertEquals(TaskStatus.COMPLETED, response[2].status)
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

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = listOf(householdId),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

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

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId, householdId2),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(allTasks)

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = listOf(householdId, householdId2),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

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
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member of household 2"),
            )

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(
                        userId,
                        TaskGetAllHandler.Request(
                            householdIds = listOf(householdId, householdId2),
                            dueDateFrom = null,
                            dueDateTo = null,
                            completedDateFrom = null,
                            completedDateTo = null,
                        ),
                    )
                }

            assertEquals("User is not a member of household 2", ex.message)
            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(householdMemberValidator).validate(context, householdId2, userId)
            verify(taskRepository, never()).findByHouseholdIdsWithDateFilters(any(), any(), any(), any(), any())
        }

    @Test
    fun `handle should return empty list when no household IDs provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = emptyList(),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            assertTrue(response.isEmpty())
            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = emptyList(),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }

    @Test
    fun `handle should return all tasks when no household IDs provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userHouseholds =
                listOf(
                    TestUtils.getHousehold(id = householdId, name = "Household 1"),
                    TestUtils.getHousehold(id = householdId2, name = "Household 2"),
                )

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Task from Household 1",
                        createdBy = userId,
                        householdId = householdId,
                    ),
                    TestUtils.getTask(
                        title = "Task from Household 2",
                        createdBy = userId,
                        householdId = householdId2,
                    ),
                )

            whenever(householdRepository.findByMemberId(userId)).thenReturn(userHouseholds)
            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId, householdId2),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = emptyList(),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            assertEquals(2, response.size)
            verify(householdRepository).findByMemberId(userId)
            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId, householdId2),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }

    @Test
    fun `handle should return empty list when user has no households`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(householdRepository.findByMemberId(userId)).thenReturn(emptyList())

            val response =
                handler.handle(
                    userId,
                    TaskGetAllHandler.Request(
                        householdIds = emptyList(),
                        dueDateFrom = null,
                        dueDateTo = null,
                        completedDateFrom = null,
                        completedDateTo = null,
                    ),
                )

            assertTrue(response.isEmpty())
            verify(householdRepository).findByMemberId(userId)
            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = emptyList(),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )
            verify(householdMemberValidator, never()).validate(any(), any(), any())
        }

    @Test
    fun `handle should filter tasks by due date range`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val dueDateFrom = Instant.parse("2023-12-01T10:00:00Z")
            val dueDateTo = Instant.parse("2023-12-31T23:59:59Z")

            val request =
                TaskGetAllHandler.Request(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = dueDateTo,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Task within range",
                        dueDate = Instant.parse("2023-12-15T12:00:00Z"),
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = dueDateTo,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)
            whenever(taskTagManager.getTasksTags(context, tasks.map { it.id })).thenReturn(emptyMap())

            val response = handler.handle(userId, request)

            assertEquals(1, response.size)
            assertEquals("Task within range", response[0].title)

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = dueDateFrom,
                dueDateTo = dueDateTo,
                completedDateFrom = null,
                completedDateTo = null,
            )
        }

    @Test
    fun `handle should filter tasks by completed date range`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val completedDateFrom = Instant.parse("2023-12-15T00:00:00Z")
            val completedDateTo = Instant.parse("2023-12-15T23:59:59Z")

            val request =
                TaskGetAllHandler.Request(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = completedDateFrom,
                    completedDateTo = completedDateTo,
                )

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Completed task within range",
                        status = TaskStatus.COMPLETED,
                        completedAt = Instant.parse("2023-12-15T12:00:00Z"),
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = null,
                    dueDateTo = null,
                    completedDateFrom = completedDateFrom,
                    completedDateTo = completedDateTo,
                ),
            ).thenReturn(tasks)
            whenever(taskTagManager.getTasksTags(context, tasks.map { it.id })).thenReturn(emptyMap())

            val response = handler.handle(userId, request)

            assertEquals(1, response.size)
            assertEquals("Completed task within range", response[0].title)
            assertEquals(TaskStatus.COMPLETED, response[0].status)

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = null,
                dueDateTo = null,
                completedDateFrom = completedDateFrom,
                completedDateTo = completedDateTo,
            )
        }

    @Test
    fun `handle should filter tasks by both due date and completed date ranges`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val dueDateFrom = Instant.parse("2023-12-01T10:00:00Z")
            val dueDateTo = Instant.parse("2023-12-31T23:59:59Z")
            val completedDateFrom = Instant.parse("2023-12-15T00:00:00Z")
            val completedDateTo = Instant.parse("2023-12-15T23:59:59Z")

            val request =
                TaskGetAllHandler.Request(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = dueDateTo,
                    completedDateFrom = completedDateFrom,
                    completedDateTo = completedDateTo,
                )

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Task with both filters",
                        dueDate = Instant.parse("2023-12-15T12:00:00Z"),
                        status = TaskStatus.COMPLETED,
                        completedAt = Instant.parse("2023-12-15T12:00:00Z"),
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = dueDateTo,
                    completedDateFrom = completedDateFrom,
                    completedDateTo = completedDateTo,
                ),
            ).thenReturn(tasks)
            whenever(taskTagManager.getTasksTags(context, tasks.map { it.id })).thenReturn(emptyMap())

            val response = handler.handle(userId, request)

            assertEquals(1, response.size)
            assertEquals("Task with both filters", response[0].title)

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = dueDateFrom,
                dueDateTo = dueDateTo,
                completedDateFrom = completedDateFrom,
                completedDateTo = completedDateTo,
            )
        }

    @Test
    fun `handle should work with partial date filters`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val dueDateFrom = Instant.parse("2023-12-01T10:00:00Z")

            val request =
                TaskGetAllHandler.Request(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                )

            val tasks =
                listOf(
                    TestUtils.getTask(
                        title = "Task after due date from",
                        dueDate = Instant.parse("2023-12-15T12:00:00Z"),
                        createdBy = userId,
                        householdId = householdId,
                    ),
                )

            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    householdIds = listOf(householdId),
                    dueDateFrom = dueDateFrom,
                    dueDateTo = null,
                    completedDateFrom = null,
                    completedDateTo = null,
                ),
            ).thenReturn(tasks)
            whenever(taskTagManager.getTasksTags(context, tasks.map { it.id })).thenReturn(emptyMap())

            val response = handler.handle(userId, request)

            assertEquals(1, response.size)
            assertEquals("Task after due date from", response[0].title)

            verify(taskRepository).findByHouseholdIdsWithDateFilters(
                householdIds = listOf(householdId),
                dueDateFrom = dueDateFrom,
                dueDateTo = null,
                completedDateFrom = null,
                completedDateTo = null,
            )
        }
}
