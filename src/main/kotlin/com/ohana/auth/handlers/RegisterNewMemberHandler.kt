package com.ohana.auth.handlers

import org.jdbi.v3.core.Jdbi
import org.koin.core.component.KoinComponent
import com.ohana.auth.utils.Hasher
import com.ohana.auth.utils.JwtCreator


class RegisterNewMemberHandler(
    private val jdbi: Jdbi
): KoinComponent {
    fun handle(request: Request): String {
        val salt = Hasher.generateSalt()
        val hashedPassword = Hasher.hashPassword(request.password, salt)

        val memberId = insertMember(request.name, request.email, hashedPassword, salt)

        return JwtCreator.generateToken(memberId)
    }

    fun insertMember(name: String, email: String, password: String, salt: ByteArray): Int {
        return jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("INSERT INTO members (name, email, password, salt) VALUES (:name, :email, :password, :salt)")
                .bind("name", name)
                .bind("email", email)
                .bind("password", password)
                .bind("salt", salt)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Int::class.java)
                .findOne()
                .orElseThrow { throw IllegalStateException("Failed to insert member") }
        }
    }

    data class Request(
        val name: String,
        val email: String,
        val password: String
    )
}
