package com.ohana.members.handlers

import com.ohana.exceptions.DbException
import com.ohana.exceptions.NotFoundException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.transaction
import com.ohana.utils.DatabaseUtils.Companion.update
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class MemberUpdateByIdHandler(
    private val jdbi: Jdbi,
) {
    data class Request(
        val name: String,
        val age: Int?,
        val gender: String?,
    )

    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(
        id: String,
        request: Request,
    ): Response =
        transaction(jdbi) { handle ->
            updateMember(handle, id, request)
            fetchMemberById(handle, id)
        }

    private fun updateMember(
        handle: Handle,
        id: String,
        request: Request,
    ) {
        val updateQuery = """
            UPDATE members
            SET name = :name,
                age = :age,
                gender = :gender
            WHERE id = :id
        """

        val updatedRows =
            update(
                handle,
                updateQuery,
                mapOf(
                    "id" to id,
                    "name" to request.name,
                    "age" to request.age,
                    "gender" to request.gender,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update member")
    }

    private fun fetchMemberById(
        handle: Handle,
        id: String,
    ): Response {
        val selectQuery = """
            SELECT id, name, age, gender, email
            FROM members
            WHERE id = :id
        """

        return get(
            handle,
            selectQuery,
            mapOf("id" to id),
            Response::class,
        ).firstOrNull() ?: throw NotFoundException("Member not found")
    }
}
