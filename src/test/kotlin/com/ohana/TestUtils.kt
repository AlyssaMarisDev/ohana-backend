package com.ohana

import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.RefreshToken
import com.ohana.data.household.Household
import com.ohana.data.household.HouseholdMember
import com.ohana.data.member.Member
import com.ohana.data.permissions.Permission
import com.ohana.data.tags.Tag
import com.ohana.data.tags.TaskTag
import com.ohana.data.task.Task
import com.ohana.data.unitOfWork.*
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.HouseholdMemberStatus
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
        ): Household = Household(id, name, description, createdBy)

        fun getHouseholdMember(
            id: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
            memberId: String = UUID.randomUUID().toString(),
            role: HouseholdMemberRole = HouseholdMemberRole.MEMBER,
            status: HouseholdMemberStatus = HouseholdMemberStatus.ACTIVE,
            isDefault: Boolean = false,
            invitedBy: String? = null,
            joinedAt: Instant? = null,
        ): HouseholdMember =
            HouseholdMember(
                id,
                householdId,
                memberId,
                role,
                status,
                isDefault,
                invitedBy,
                joinedAt,
            )

        fun getTask(
            id: String = UUID.randomUUID().toString(),
            title: String = "Test Task",
            description: String = "A test task",
            dueDate: Instant? = null,
            status: TaskStatus = TaskStatus.PENDING,
            completedAt: Instant? = null,
            createdBy: String = UUID.randomUUID().toString(),
            householdId: String = UUID.randomUUID().toString(),
        ): Task =
            Task(
                id,
                title,
                description,
                dueDate,
                status,
                completedAt,
                createdBy,
                householdId,
            )

        fun getTag(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test Tag",
            color: String = "#FF0000",
            householdId: String = UUID.randomUUID().toString(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = Instant.now(),
        ): Tag = Tag(id, name, color, householdId, createdAt, updatedAt)

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
            name: String = "Test User",
            email: String = "test@example.com",
            password: String = "hashedPassword",
            salt: ByteArray = "salt".toByteArray(),
            age: Int? = null,
            gender: String? = null,
        ): AuthMember = AuthMember(id, name, email, password, salt, age, gender)

        fun getMember(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test Member",
            email: String = "member@example.com",
            age: Int? = null,
            gender: String? = null,
        ): Member = Member(id, name, email, age, gender)

        fun getRefreshToken(
            id: String = UUID.randomUUID().toString(),
            token: String = "refresh-token-123",
            userId: String = UUID.randomUUID().toString(),
            expiresAt: Instant = Instant.now().plusSeconds(3600),
            isRevoked: Boolean = false,
            createdAt: Instant = Instant.now(),
            revokedAt: Instant? = null,
        ): RefreshToken = RefreshToken(id, token, userId, expiresAt, isRevoked, createdAt, revokedAt)

        suspend fun mockUnitOfWork(
            unitOfWork: UnitOfWork,
            context: UnitOfWorkContext,
        ) {
            whenever(unitOfWork.execute(any<(UnitOfWorkContext) -> Any>())).thenAnswer { invocation ->
                val block = invocation.getArgument<(UnitOfWorkContext) -> Any>(0)
                block(context)
            }
        }
    }
}
