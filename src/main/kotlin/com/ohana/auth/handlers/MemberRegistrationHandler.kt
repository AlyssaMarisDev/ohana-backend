package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.ConflictException
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.insert
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.util.UUID

class MemberRegistrationHandler(
    private val jdbi: Jdbi,
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

        return transaction(jdbi) { handle ->
            // Check if the member already exists
            val existingMember = findMemberByEmail(handle, request.email)

            if (existingMember != null) {
                throw ConflictException("Member with email ${request.email} already exists")
            }

            // Insert the new member
            insertMember(handle, id, request.name, request.email, hashedPassword, salt)

            Response(id, JwtCreator.generateToken(id))
        }
    }

    private fun insertMember(
        handle: Handle,
        id: String,
        name: String,
        email: String,
        password: String,
        salt: ByteArray,
    ): Int =
        insert(
            handle,
            """
            INSERT INTO members (id, name, email, password, salt)
            VALUES (:id, :name, :email, :password, :salt)
            """,
            mapOf(
                "id" to id,
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
        get(
            handle,
            "SELECT id FROM members WHERE email = :email",
            mapOf("email" to email),
            Member::class,
        ).firstOrNull()

    private data class Member(
        val id: String,
    )
}
