package com.ohana.data.auth

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle

class JdbiAuthMemberRepository(
    private val handle: Handle,
) : AuthMemberRepository {
    override fun findByEmail(email: String): AuthMember? {
        val selectQuery = """
            SELECT id, name, email, password, salt, age, gender
            FROM members
            WHERE email = :email
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("email" to email),
                AuthMember.mapper,
            ).firstOrNull()
    }

    override fun create(member: AuthMember): AuthMember {
        val insertQuery = """
            INSERT INTO members (id, name, email, password, salt, age, gender)
            VALUES (:id, :name, :email, :password, :salt, :age, :gender)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "email" to member.email,
                    "password" to member.password,
                    "salt" to member.salt,
                    "age" to member.age,
                    "gender" to member.gender,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create member")

        return findByEmail(member.email) ?: throw NotFoundException("Member not found after creation")
    }
}
