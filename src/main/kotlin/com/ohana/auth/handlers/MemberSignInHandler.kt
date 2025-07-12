package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.AuthorizationException
import com.ohana.shared.UnitOfWork

class MemberSignInHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val email: String,
        val password: String,
    )

    data class Response(
        val token: String,
    )

    suspend fun handle(request: Request): Response =
        unitOfWork.execute { context ->
            val member = context.authMembers.findByEmail(request.email)

            if (member == null || member.password != Hasher.hashPassword(request.password, member.salt)) {
                throw AuthorizationException("Invalid email or password")
            }

            Response(JwtCreator.generateToken(member.id))
        }
}
