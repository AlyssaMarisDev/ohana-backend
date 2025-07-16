package com.ohana

import com.ohana.data.household.Household
import com.ohana.data.household.HouseholdMember
import com.ohana.data.member.Member
import com.ohana.data.task.Task
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.TaskStatus
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class TestUtils {
    companion object {
        suspend fun mockUnitOfWork(
            unitOfWork: UnitOfWork,
            context: UnitOfWorkContext,
        ) {
            whenever(unitOfWork.execute<Any>(any())).thenAnswer { invocation ->
                val block = invocation.getArgument<(UnitOfWorkContext) -> Any>(0)
                block.invoke(context)
            }
        }

        fun getTask(
            id: String = UUID.randomUUID().toString(),
            title: String = "Test Task",
            description: String = "Test Description",
            dueDate: Instant = Instant.now(),
            status: TaskStatus = TaskStatus.pending,
            createdBy: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
        ): Task = Task(id, title, description, dueDate, status, createdBy, householdId)

        fun getMember(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test User",
            email: String = "test@example.com",
            age: Int? = null,
            gender: String? = null,
        ): Member = Member(id, name, email, age, gender)

        fun getHousehold(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test Household",
            description: String = "Test household description",
            createdBy: String = UUID.randomUUID().toString(),
        ): Household = Household(id, name, description, createdBy)

        fun getHouseholdMember(
            id: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
            memberId: String = UUID.randomUUID().toString(),
            role: HouseholdMemberRole = HouseholdMemberRole.member,
            isActive: Boolean = true,
            invitedBy: String? = UUID.randomUUID().toString(),
            joinedAt: Instant? = Instant.now(),
        ): HouseholdMember =
            HouseholdMember(
                id = id,
                householdId = householdId,
                memberId = memberId,
                role = role,
                isActive = isActive,
                invitedBy = invitedBy,
                joinedAt = joinedAt,
            )
    }
}
