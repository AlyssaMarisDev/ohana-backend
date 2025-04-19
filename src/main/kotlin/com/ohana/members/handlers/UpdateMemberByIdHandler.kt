package com.ohana.members.handlers

import com.ohana.exceptions.DbException
import com.ohana.exceptions.NotFoundException
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class UpdateMemberByIdHandler(
    private val jdbi: Jdbi,
) {
    suspend fun handle(
        id: Int,
        request: Request,
    ): Response =
        transaction(jdbi) { handle ->
            // Fetch to check if the member exists
            fetchMemberById(handle, id)

            // Perform the update
            val updatedRows = updateMember(handle, id, request)

            if (updatedRows == 0) {
                throw DbException("Failed to update member")
            }

            // Return the updated member
            fetchMemberById(handle, id)
        }

    private fun updateMember(
        handle: Handle,
        id: Int,
        request: Request,
    ): Int {
        val updateQuery = """
            UPDATE members
            SET name = :name,
                age = :age,
                gender = :gender
            WHERE id = :id
        """

        return handle
            .createUpdate(updateQuery)
            .bind("id", id)
            .bind("name", request.name)
            .bind("age", request.age)
            .bind("gender", request.gender)
            .execute()
    }

    private fun fetchMemberById(
        handle: Handle,
        id: Int,
    ): Response {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
            WHERE id = :id
        """

        return handle
            .createQuery(selectQuery)
            .bind("id", id)
            .map { rs, _ ->
                Response(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    age = if (rs.wasNull()) null else rs.getInt("age"),
                    gender = if (rs.wasNull()) null else rs.getString("gender"),
                    email = rs.getString("email"),
                )
            }.findOne()
            .orElseThrow { throw NotFoundException("Member not found") }
    }

    data class Request(
        val name: String,
        val age: Int?,
        val gender: String?,
    )

    data class Response(
        val id: Int,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )
}
