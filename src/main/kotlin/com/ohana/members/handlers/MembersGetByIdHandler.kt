package com.ohana.members.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class MembersGetByIdHandler(
    private val jdbi: Jdbi,
) {
    data class Response(
        val id: Int,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(id: Int): Response =
        query(jdbi) { handle ->
            fetchSingleMemberById(handle, id) ?: throw NotFoundException("Member not found")
        }

    fun fetchSingleMemberById(
        handle: Handle,
        id: Int,
    ): Response? =
        get(
            handle,
            "SELECT id, name, age, gender, email FROM members WHERE id = :id",
            mapOf("id" to id),
            Response::class,
        ).firstOrNull()
}
