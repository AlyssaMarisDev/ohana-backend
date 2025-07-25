package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.member.MemberRepository
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.AuthorizationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: TaskCreationHandler
    private lateinit var householdMemberValidator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        memberRepository = mock()
        householdMemberValidator = mock()
        taskTagManager = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = TaskCreationHandler(unitOfWork, householdMemberValidator, taskTagManager)
    }

    @Test
    fun `handle should create task when validation passes`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                TaskCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(1000),
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            val task =
                TestUtils.getTask(
                    id = request.id,
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                    createdBy = userId,
                    householdId = householdId,
                )

            whenever(taskRepository.create(any())).thenReturn(task)
            whenever(taskTagManager.assignTagsToTask(context, task.id, request.tagIds)).thenReturn(emptyList())

            val response = handler.handle(userId, request)

            verify(householdMemberValidator).validate(context, householdId, userId)
            verify(taskRepository).create(task)
            verify(taskTagManager).assignTagsToTask(context, task.id, request.tagIds)

            assertEquals(task.id, response.id)
            assertEquals(task.title, response.title)
            assertEquals(task.description, response.description)
            assertEquals(task.dueDate, response.dueDate)
            assertEquals(task.status, response.status)
            assertEquals(task.createdBy, response.createdBy)
            assertEquals(task.householdId, response.householdId)
            assertEquals(0, response.tags.size)
        }

    @Test
    fun `handle should throw when validation fails`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val request =
                TaskCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(1000),
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            whenever(
                householdMemberValidator.validate(context, householdId, userId),
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

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val request =
                TaskCreationHandler.Request(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = "Test Description",
                    dueDate = Instant.now().plusSeconds(1000),
                    status = TaskStatus.PENDING,
                    householdId = householdId,
                    tagIds = emptyList(),
                )

            whenever(taskRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, request)
                }
            assertEquals("DB error", ex.message)
        }
}
