package com.ohana.members.handlers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.Handle
import com.ohana.utils.TransactionHandler.Companion.transaction

class UpdateMemberByIdHandler(private val jdbi: Jdbi) {
    suspend fun handle(id: Int, request: Request): Response? {
        return transaction(jdbi) { handle ->
            // Perform the update
            val updatedRows = updateMember(handle, id, request)

            if (updatedRows == 0) {
                throw Exception("Failed to update member")
            }

            // Fetch the updated row
            fetchUpdatedMember(handle, id)
        }
    }

    private fun updateMember(handle: Handle, id: Int, request: Request): Int {
        val updateQuery = """
            UPDATE members
            SET name = :name,
                age = :age,
                gender = :gender
            WHERE id = :id
        """

        return handle.createUpdate(updateQuery)
            .bind("id", id)
            .bind("name", request.name)
            .bind("age", request.age)
            .bind("gender", request.gender)
            .execute()
    }

    private fun fetchUpdatedMember(handle: Handle, id: Int): Response? {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
            WHERE id = :id
        """

        return handle.createQuery(selectQuery)
            .bind("id", id)
            .map { rs, _ ->
                Response(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    age = if (rs.wasNull()) null else rs.getInt("age"),
                    gender = if (rs.wasNull()) null else rs.getString("gender"),
                    email = rs.getString("email")
                )
            }
            .findOne()
            .orElse(null)
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
        val email: String
    )
}

