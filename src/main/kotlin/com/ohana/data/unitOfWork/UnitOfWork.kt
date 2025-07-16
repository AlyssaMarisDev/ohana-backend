package com.ohana.data.unitOfWork

import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.member.MemberRepository
import com.ohana.data.task.TaskRepository

// Unit of Work interface
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

// Unit of Work context interface
interface UnitOfWorkContext {
    val tasks: TaskRepository
    val members: MemberRepository
    val households: HouseholdRepository
    val authMembers: AuthMemberRepository
}
