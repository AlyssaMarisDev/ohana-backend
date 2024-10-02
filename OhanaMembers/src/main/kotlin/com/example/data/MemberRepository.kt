package com.example.data

import com.example.models.Member

class MemberRepository {
    private val members = mutableListOf(
        Member(1, "Brandon", 37, "enby", "brandon@fiquett.com", "password"),
        Member(2, "Bree", 27, "female", "alyssamarisdev@gmail.com", "password"),
    )

    fun getAllMembers(): List<Member> = members

    fun getMemberById(id: Int): Member? = members.find { it.id == id }

    fun addMember(member: Member) {
        members.add(member)
    }

    fun deleteMember(id: Int) {
        members.removeIf { it.id == id }
    }
}