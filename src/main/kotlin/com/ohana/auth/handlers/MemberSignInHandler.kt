package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.AuthorizationException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Jdbi

class MemberSignInHandler(
    private val jdbi: Jdbi,
) {
    data class Request(
        val email: String,
        val password: String,
    )

    data class Response(
        val token: String,
    )

    suspend fun handle(request: Request): Response {
        val member = getMemberByEmail(request.email)

        if (member == null || member.password != Hasher.hashPassword(request.password, member.salt)) {
            throw AuthorizationException("Invalid email or password")
        }

        return Response(JwtCreator.generateToken(member.id))
    }

    private suspend fun getMemberByEmail(email: String): Member? =
        query(jdbi) { handle ->
            get(
                handle,
                "SELECT id, password, salt FROM members WHERE email = :email",
                mapOf("email" to email),
                Member::class,
            ).firstOrNull()
        }

    private data class Member(
        val id: String,
        val password: String,
        val salt: ByteArray,
    )
}
