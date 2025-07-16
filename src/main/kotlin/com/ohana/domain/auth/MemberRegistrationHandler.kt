package com.ohana.domain.auth

import com.ohana.data.auth.AuthMember
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.*
import com.ohana.shared.exceptions.ConflictException
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

class MemberRegistrationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Name is required")
        @field:Size(min = 3, message = "Name must be at least 3 characters long")
        val name: String,
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Invalid email format")
        val email: String,
        @field:NotBlank(message = "Password is required")
        @field:Size(min = 8, message = "Password must be at least 8 characters long")
        @field:Pattern(
            regexp = ".*[A-Z].*",
            message = "Password must contain at least one uppercase letter",
        )
        @field:Pattern(
            regexp = ".*[0-9].*",
            message = "Password must contain at least one number",
        )
        @field:Pattern(
            regexp = ".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>\\/?].*",
            message = "Password must contain at least one special character",
        )
        val password: String,
    )

    data class Response(
        val id: String,
        val token: String,
    )

    suspend fun handle(request: Request): Response {
        val salt = Hasher.generateSalt()
        val hashedPassword = Hasher.hashPassword(request.password, salt)
        val id = UUID.randomUUID().toString()

        return unitOfWork.execute { context ->
            // Validate that member doesn't already exist
            val existingMember = context.authMembers.findByEmail(request.email)
            if (existingMember != null) {
                throw ConflictException("Member with email ${request.email} already exists")
            }

            // Create the member
            val member =
                context.authMembers.create(
                    AuthMember(
                        id = id,
                        name = request.name,
                        email = request.email,
                        password = hashedPassword,
                        salt = salt,
                    ),
                )

            Response(member.id, JwtCreator.generateToken(member.id))
        }
    }
}
