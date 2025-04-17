package com.ohana.handlers

import com.ohana.data.MemberRepository
import com.ohana.models.Member

class GetSingleMemberService (private val memberRepository : MemberRepository) {
    suspend fun run(id: Int) : Member? {
        return memberRepository.getMemberById(id)
    }
}