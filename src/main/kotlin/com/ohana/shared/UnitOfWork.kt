package com.ohana.shared

import com.ohana.auth.entities.AuthMember
import com.ohana.member.entities.Member
import com.ohana.task.entities.Task

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

// Repository interfaces
interface TaskRepository {
    fun create(task: Task): Task

    fun findById(id: String): Task?

    fun findAll(): List<Task>

    fun findByHouseholdId(householdId: String): List<Task>

    fun update(task: Task): Task
}

interface MemberRepository {
    fun findById(id: String): Member?

    fun findAll(): List<Member>

    fun findByEmail(email: String): Member?

    fun create(member: Member): Member

    fun update(member: Member): Member
}

interface HouseholdRepository {
    fun findById(id: String): Household?

    fun findAll(): List<Household>

    fun create(household: Household): Household

    fun findMemberById(
        householdId: String,
        memberId: String,
    ): HouseholdMember?

    fun createMember(member: HouseholdMember): HouseholdMember

    fun updateMember(member: HouseholdMember): HouseholdMember
}

interface AuthMemberRepository {
    fun findByEmail(email: String): AuthMember?

    fun create(member: AuthMember): AuthMember
}

// Data classes for household operations
data class Household(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
)

data class HouseholdMember(
    val id: String,
    val householdId: String,
    val memberId: String,
    val role: HouseholdMemberRole,
    val isActive: Boolean = false,
    val invitedBy: String? = null,
    val joinedAt: java.time.Instant? = null,
)
