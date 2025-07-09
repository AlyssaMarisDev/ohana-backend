package com.ohana.members.handlers

import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class MembersGetAllHandler(
    private val jdbi: Jdbi,
) {
    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(): List<Response> {
        val response =
            query(jdbi) { handle ->
                fetchAllMembers(handle)
            }

        return response
    }

    fun fetchAllMembers(handle: Handle): List<Response> =
        get(
            handle,
            "SELECT id, name, age, gender, email FROM members",
            mapOf(),
            Response::class,
        )
}
