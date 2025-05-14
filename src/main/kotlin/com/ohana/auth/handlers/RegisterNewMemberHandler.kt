package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.ConflictException
import com.ohana.utils.DatabaseUtils.Companion.fetch
import com.ohana.utils.DatabaseUtils.Companion.insert
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.koin.core.component.KoinComponent

class RegisterNewMemberHandler(
    private val jdbi: Jdbi,
) : KoinComponent {
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
        val id: Int,
        val token: String,
    )

    suspend fun handle(request: Request): Response {
        val salt = Hasher.generateSalt()
        val hashedPassword = Hasher.hashPassword(request.password, salt)

        return transaction(jdbi) { handle ->
            // Check if the member already exists
            val existingMember = findMemberByEmail(handle, request.email)

            if (existingMember != null) {
                throw ConflictException("Member with email ${request.email} already exists")
            }

            // Insert the new member
            val memberId = insertMember(handle, request.name, request.email, hashedPassword, salt)

            Response(memberId, JwtCreator.generateToken(memberId))
        }
    }

    private fun insertMember(
        handle: Handle,
        name: String,
        email: String,
        password: String,
        salt: ByteArray,
    ): Int =
        insert(
            handle,
            "INSERT INTO members (name, email, password, salt) VALUES (:name, :email, :password, :salt)",
            mapOf(
                "name" to name,
                "email" to email,
                "password" to password,
                "salt" to salt,
            ),
        )

    private fun findMemberByEmail(
        handle: Handle,
        email: String,
    ): Member? =
        fetch(
            handle,
            "SELECT id FROM members WHERE email = :email",
            mapOf("email" to email),
            Member::class,
        ).firstOrNull()

    private data class Member(
        val id: Int,
    )
}
