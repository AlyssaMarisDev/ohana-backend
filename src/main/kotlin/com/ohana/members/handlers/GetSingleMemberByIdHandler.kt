package com.ohana.members.handlers

import org.jdbi.v3.core.Jdbi

class GetSingleMemberByIdHandler (private val jdbi: Jdbi) {
    suspend fun handle(id: Int) : Response? {
        val response = jdbi.withHandle<Response, Exception> { handle ->
            handle.createQuery("SELECT id, name, age, gender, email FROM members WHERE id = :id")
                .bind("id", id)
                .map { rs, _ ->
                    Response(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        age = rs.getInt("age"),
                        gender = rs.getString("gender"),
                        email = rs.getString("email")
                    )
                }
                .findFirst()
                .orElse(null)
        }

        return response
    }
    
    data class Response(
        val id: Int,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )
}