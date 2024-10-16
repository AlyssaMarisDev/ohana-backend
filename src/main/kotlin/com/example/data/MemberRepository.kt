package com.example.data

import com.example.models.Member
import com.example.models.MemberEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MemberRepository {
    suspend fun getMemberById(id: Int): Member? = newSuspendedTransaction {
        MemberEntity.findById(id)?.toMember()
    }
}