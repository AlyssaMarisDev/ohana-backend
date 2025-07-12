package com.ohana.task.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.AuthorizationException
import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.MemberRepository
import com.ohana.shared.TaskRepository
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

class TaskCreationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: TaskCreationHandler
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private val userId = "user-1"

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        memberRepository = mock()
        householdMemberValidator = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = TaskCreationHandler(unitOfWork, householdMemberValidator)
    }

    @Test
    fun `handle should create task when validation passes`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val request =
                TaskCreationHandler.Request(
                    id = "task-1",
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now(),
                    status = TaskStatus.pending,
                    householdId = "household-1",
                )

            val task = TestUtils.getTask()

            whenever(taskRepository.create(any())).thenReturn(task)

            val response = handler.handle(userId, request)

            verify(householdMemberValidator).validate(context, "household-1", userId)
            verify(taskRepository).create(task)

            assertEquals(task.id, response.id)
            assertEquals(task.title, response.title)
            assertEquals(task.description, response.description)
            assertEquals(task.dueDate, response.dueDate)
            assertEquals(task.status, response.status)
            assertEquals(task.createdBy, response.createdBy)
            assertEquals(task.householdId, response.householdId)
        }

    @Test
    fun `handle should throw when validation fails`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                TaskCreationHandler.Request(
                    id = "task-1",
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now(),
                    status = TaskStatus.pending,
                    householdId = "household-1",
                )

            whenever(
                householdMemberValidator.validate(context, "household-1", userId),
            ).thenThrow(AuthorizationException("User is not a member of the household"))

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, request)
                }
            assertEquals("User is not a member of the household", ex.message)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val request =
                TaskCreationHandler.Request(
                    id = "task-1",
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now(),
                    status = TaskStatus.pending,
                    householdId = "household-1",
                )

            whenever(taskRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }
            assertEquals("DB error", ex.message)
        }
}
