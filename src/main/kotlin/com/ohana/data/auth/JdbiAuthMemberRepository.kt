package com.ohana.data.auth

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiAuthMemberRepository(
    private val handle: Handle,
) : AuthMemberRepository {
    init {
        handle.registerRowMapper(AuthMemberRowMapper())
    }

    override fun findByEmail(email: String): AuthMember? {
        val selectQuery = """
            SELECT id, name, email, password, salt, age, gender
            FROM members
            WHERE email = :email
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("email" to email),
                AuthMember::class,
            ).firstOrNull()
    }

    private class AuthMemberRowMapper : RowMapper<AuthMember> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): AuthMember =
            AuthMember(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                password = rs.getString("password"),
                salt = rs.getBytes("salt"),
                age = rs.getObject("age") as? Int,
                gender = rs.getString("gender"),
            )
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
