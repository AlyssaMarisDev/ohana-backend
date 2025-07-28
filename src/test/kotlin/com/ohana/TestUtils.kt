package com.ohana

import com.ohana.data.auth.AuthMember
import com.ohana.data.household.Household
import com.ohana.data.household.HouseholdMember
import com.ohana.data.permissions.Permission
import com.ohana.data.tags.Tag
import com.ohana.data.tags.TaskTag
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
        fun getHousehold(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test Household",
            description: String = "A test household",
            createdBy: String = UUID.randomUUID().toString(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): Household = Household(id, name, description, createdBy, createdAt, updatedAt)

        fun getHouseholdMember(
            id: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
            memberId: String = UUID.randomUUID().toString(),
            role: HouseholdMemberRole = HouseholdMemberRole.MEMBER,
            isActive: Boolean = true,
            isDefault: Boolean = false,
            invitedBy: String = UUID.randomUUID().toString(),
            joinedAt: Instant = Instant.now(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): HouseholdMember =
            HouseholdMember(
                id,
                householdId,
                memberId,
                role,
                isActive,
                isDefault,
                invitedBy,
                joinedAt,
                createdAt,
                updatedAt,
            )

        fun getTask(
            id: String = UUID.randomUUID().toString(),
            title: String = "Test Task",
            description: String = "A test task",
            status: TaskStatus = TaskStatus.TODO,
            priority: Int = 1,
            dueDate: Instant? = null,
            assignedTo: String? = null,
            createdBy: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): Task =
            Task(
                id,
                title,
                description,
                status,
                priority,
                dueDate,
                assignedTo,
                createdBy,
                householdId,
                createdAt,
                updatedAt,
            )

        fun getTag(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test Tag",
            color: String = "#FF0000",
            householdId: String? = null,
            isDefault: Boolean = false,
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): Tag = Tag(id, name, color, householdId, isDefault, createdAt, updatedAt)

        fun getTaskTag(
            id: String = UUID.randomUUID().toString(),
            taskId: String = UUID.randomUUID().toString(),
            tagId: String = UUID.randomUUID().toString(),
            createdAt: Instant = Instant.now(),
        ): TaskTag = TaskTag(id, taskId, tagId, createdAt)

        fun getPermission(
            id: String = UUID.randomUUID().toString(),
            householdMemberId: String = UUID.randomUUID().toString(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): Permission = Permission(id, householdMemberId, createdAt, updatedAt)

        fun getAuthMember(
            id: String = UUID.randomUUID().toString(),
            email: String = "test@example.com",
            passwordHash: String = "hashedPassword",
            salt: String = "salt",
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): AuthMember = AuthMember(id, email, passwordHash, salt, createdAt, updatedAt)

        fun mockUnitOfWork(
            unitOfWork: com.ohana.data.unitOfWork.UnitOfWork,
            context: com.ohana.data.unitOfWork.UnitOfWorkContext,
        ) {
            whenever(unitOfWork.execute(any())).thenAnswer { invocation ->
                val block = invocation.getArgument<suspend (com.ohana.data.unitOfWork.UnitOfWorkContext) -> Any>(0)
                block(context)
            }
        }
    }
}
