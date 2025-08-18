package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.task.TaskRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class TaskGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var handler: TaskGetAllHandler

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        householdRepository = mock()
        validator = mock()
        taskTagManager = mock()
        tagPermissionManager = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { households } doReturn householdRepository
            }
        unitOfWork = mock()
        handler = TaskGetAllHandler(unitOfWork, validator, taskTagManager, tagPermissionManager)
    }

    @Test
    fun `handle should filter tasks by tag permissions`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)
            val userId = UUID.randomUUID().toString()
            val h1 = TestUtils.getHousehold(id = UUID.randomUUID().toString(), createdBy = userId)
            val h2 = TestUtils.getHousehold(id = UUID.randomUUID().toString(), createdBy = userId)
            val member1 = TestUtils.getHouseholdMember(householdId = h1.id, memberId = userId)
            val member2 = TestUtils.getHouseholdMember(householdId = h2.id, memberId = userId)

            whenever(householdRepository.findByMemberId(userId)).thenReturn(listOf(h1, h2))
            whenever(householdRepository.findMemberById(h1.id, userId)).thenReturn(member1)
            whenever(householdRepository.findMemberById(h2.id, userId)).thenReturn(member2)

            val t1 = TestUtils.getTask(householdId = h1.id)
            val t2 = TestUtils.getTask(householdId = h2.id)
            whenever(
                taskRepository.findByHouseholdIdsWithDateFilters(
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                ),
            ).thenReturn(listOf(t1, t2))

            whenever(tagPermissionManager.filterTasksByTagPermissions(eq(context), eq(member1.id), eq(listOf(t1.id))))
                .thenReturn(listOf(t1.id))
            whenever(tagPermissionManager.filterTasksByTagPermissions(eq(context), eq(member2.id), eq(listOf(t2.id))))
                .thenReturn(emptyList())

            whenever(taskTagManager.getTasksTags(eq(context), eq(listOf(t1.id))))
                .thenReturn(mapOf(t1.id to emptyList()))

            val res = handler.handle(userId, TaskGetAllHandler.Request(emptyList(), null, null, null, null))
            assertEquals(1, res.size)
            assertEquals(t1.id, res[0].id)
        }
}
