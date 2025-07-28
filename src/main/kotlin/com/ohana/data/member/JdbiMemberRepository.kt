package com.ohana.data.member

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiMemberRepository(
    private val handle: Handle,
) : MemberRepository {
    init {
        handle.registerRowMapper(MemberRowMapper())
    }

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

    override fun findAll(): List<Member> {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf(),
                Member::class,
            )
    }

    override fun findByEmail(email: String): Member? {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
            WHERE email = :email
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("email" to email),
                Member::class,
            ).firstOrNull()
    }

    override fun findByHouseholdId(householdId: String): List<Member> {
        val selectQuery = """
            SELECT m.id, m.name, m.age, m.gender, m.email
            FROM members m
            INNER JOIN household_members hm ON m.id = hm.member_id
            WHERE hm.household_id = :householdId AND hm.is_active = true
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("householdId" to householdId),
                Member::class,
            )
    }

    override fun create(member: Member): Member {
        val insertQuery = """
            INSERT INTO members (id, name, age, gender, email)
            VALUES (:id, :name, :age, :gender, :email)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "age" to member.age,
                    "gender" to member.gender,
                    "email" to member.email,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create member")

        return findById(member.id) ?: throw NotFoundException("Member not found after creation")
    }

    override fun update(member: Member): Member {
        val updateQuery = """
            UPDATE members
            SET name = :name, age = :age, gender = :gender, email = :email
            WHERE id = :id
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "age" to member.age,
                    "gender" to member.gender,
                    "email" to member.email,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update member")

        return findById(member.id) ?: throw NotFoundException("Member not found after update")
    }

    private class MemberRowMapper : RowMapper<Member> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Member =
            Member(
                id = rs.getString("id"),
                name = rs.getString("name"),
                age = rs.getObject("age") as? Int,
                gender = rs.getString("gender"),
                email = rs.getString("email"),
            )
    }
}
