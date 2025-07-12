package com.ohana.member.repositories

import com.ohana.member.entities.Member
import com.ohana.shared.MemberRepository
import com.ohana.utils.DatabaseUtils
import org.jdbi.v3.core.Handle

class JdbiMemberRepository(
    private val handle: Handle,
) : MemberRepository {
    override fun findById(id: String): Member? {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
            WHERE id = :id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("id" to id),
                Member::class,
            ).firstOrNull()
    }
}
