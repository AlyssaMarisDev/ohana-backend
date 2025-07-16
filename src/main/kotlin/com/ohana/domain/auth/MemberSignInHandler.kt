package com.ohana.domain.auth

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.AuthorizationException
import com.ohana.shared.UnitOfWork
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class MemberSignInHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Invalid email format")
        val email: String,
        @field:NotBlank(message = "Password is required")
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
