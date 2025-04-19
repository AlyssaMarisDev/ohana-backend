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
    )

    data class Response(
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

            Response(JwtCreator.generateToken(memberId))
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
