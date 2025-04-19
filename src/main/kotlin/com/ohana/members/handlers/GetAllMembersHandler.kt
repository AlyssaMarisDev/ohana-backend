package com.ohana.members.handlers

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.Handle
import com.ohana.utils.TransactionHandler.Companion.query

class GetAllMembersHandler(private val jdbi: Jdbi) {
    suspend fun handle(): List<Member> {
        val response = query(jdbi) { handle ->
            fetchAllMembers(handle)
        }

        return response
    }

    fun fetchAllMembers(handle: Handle): List<Member> {
        return handle.createQuery("SELECT id, name, age, gender, email FROM members")
        .map { rs, _ ->
            Member(
                id = rs.getInt("id"),
                name = rs.getString("name"),
                age = rs.getInt("age"),
                gender = rs.getString("gender"),
                email = rs.getString("email")
            )
        }
        .toList()
    }

    data class Member(
        val id: Int,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String
    )   
}

