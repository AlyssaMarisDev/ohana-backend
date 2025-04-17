package com.ohana.data

import com.ohana.models.Member
import com.ohana.models.MemberEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MemberRepository {
    suspend fun getMemberById(id: Int): Member? = newSuspendedTransaction {
        MemberEntity.findById(id)?.toMember()
    }
}