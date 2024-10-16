package com.example.services

import com.example.data.MemberRepository
import com.example.models.Member

class GetSingleMemberService (private val memberRepository : MemberRepository) {
    suspend fun run(id: Int) : Member? {
        return memberRepository.getMemberById(id)
    }
}