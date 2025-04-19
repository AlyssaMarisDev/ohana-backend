package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator
import com.ohana.exceptions.ConflictException
import com.ohana.exceptions.DbException
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.koin.core.component.KoinComponent

class RegisterNewMemberHandler(
    private val jdbi: Jdbi,
) : KoinComponent {
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
        handle
            .createUpdate("INSERT INTO members (name, email, password, salt) VALUES (:name, :email, :password, :salt)")
            .bind("name", name)
            .bind("email", email)
            .bind("password", password)
            .bind("salt", salt)
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .findOne()
            .orElseThrow { throw DbException("Failed to insert member") }

    private fun findMemberByEmail(
        handle: Handle,
        email: String,
    ): Int? =
        handle
            .createQuery("SELECT id FROM members WHERE email = :email")
            .bind("email", email)
            .mapTo(Int::class.java)
            .findFirst()
            .orElse(null)

    data class Request(
        val name: String,
        val email: String,
        val password: String,
    )

    data class Response(
        val token: String,
    )
}
