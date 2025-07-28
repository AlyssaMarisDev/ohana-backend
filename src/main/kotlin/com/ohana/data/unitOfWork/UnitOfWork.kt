package com.ohana.data.unitOfWork

import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.member.MemberRepository
import com.ohana.data.permissions.PermissionRepository
import com.ohana.data.permissions.TagPermissionRepository
import com.ohana.data.tags.TagRepository
import com.ohana.data.tags.TaskTagRepository
import com.ohana.data.task.TaskRepository

// Unit of Work interface
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

// Unit of Work context interface
interface UnitOfWorkContext {
    val tasks: TaskRepository
    val taskTags: TaskTagRepository
    val members: MemberRepository
    val households: HouseholdRepository
    val tags: TagRepository
    val permissions: PermissionRepository
    val tagPermissions: TagPermissionRepository
    val authMembers: AuthMemberRepository
    val refreshTokens: RefreshTokenRepository
}
