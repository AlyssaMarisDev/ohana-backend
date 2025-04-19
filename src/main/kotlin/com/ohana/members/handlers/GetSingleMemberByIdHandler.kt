package com.ohana.members.handlers

import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class GetSingleMemberByIdHandler(
    private val jdbi: Jdbi,
) {
    suspend fun handle(id: Int): Response? {
        val response =
            query(jdbi) { handle ->
                fetchSingleMemberById(handle, id)
            }

        return response
    }

    fun fetchSingleMemberById(
        handle: Handle,
        id: Int,
    ): Response? =
        handle
            .createQuery("SELECT id, name, age, gender, email FROM members WHERE id = :id")
            .bind("id", id)
            .map { rs, _ ->
                Response(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    age = rs.getInt("age"),
                    gender = rs.getString("gender"),
                    email = rs.getString("email"),
                )
            }.findFirst()
            .orElse(null)

    data class Response(
        val id: Int,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )
}
