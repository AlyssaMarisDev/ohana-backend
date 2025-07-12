package com.ohana.auth.handlers

import com.ohana.auth.entities.AuthMember
import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.ConflictException
import com.ohana.shared.UnitOfWork
import java.util.UUID

class MemberRegistrationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val name: String,
        val email: String,
        val password: String,
    ) {
        fun validate(): List<String> {
            val errors = mutableListOf<String>()

            if (name.isEmpty()) errors.add("Name is required")
            if (name.length < 3) errors.add("Name must be at least 3 characters long")
            if (email.isEmpty()) errors.add("Email is required")
            if (!email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"))) {
                errors.add("Invalid email format")
            }
            if (password.isEmpty()) errors.add("Password is required")
            if (password.length < 8) errors.add("Password must be at least 8 characters long")
            if (!password.matches(Regex(".*[A-Z].*"))) errors.add("Password must contain at least one uppercase letter")
            if (!password.matches(Regex(".*[0-9].*"))) errors.add("Password must contain at least one number")
            if (!password.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>\\/?].*"))) {
                errors.add("Password must contain at least one special character")
            }

            return errors
        }
    }

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
