package com.ohana.data.member

interface MemberRepository {
    fun findById(id: String): Member?

    fun findAll(): List<Member>

    fun findByEmail(email: String): Member?

    fun findByHouseholdId(householdId: String): List<Member>

    fun create(member: Member): Member

    fun update(member: Member): Member
}
