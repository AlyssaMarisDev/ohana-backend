package com.ohana.auth.handlers

import com.ohana.auth.utils.Hasher
import com.ohana.auth.exceptions.AuthorizationException
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.reflect.ColumnName
import com.ohana.auth.utils.JwtCreator
import org.koin.core.component.KoinComponent

class MemberSignInHandler(
    private val jdbi: Jdbi
): KoinComponent {
    fun handle(request: Request): String {
        val member = findMemberByEmail(request.email)
        
        if (member == null || member.password != Hasher.hashPassword(request.password, member.salt)) {
            throw AuthorizationException("Invalid email or password")
        }

        return JwtCreator.generateToken(member.id)
    }

    private fun findMemberByEmail(email: String): Member? {
        return jdbi.withHandle<Member, Exception> { handle ->
            handle.createQuery("SELECT id, password, salt FROM members WHERE email = :email")
                .bind("email", email)
                .map { rs, _ ->
                    Member(
                        id = rs.getInt("id"),
                        password = rs.getString("password"),
                        salt = rs.getBytes("salt")
                    )
                }
                .findOne()
                .orElse(null)
        }
    }

    data class Member(
        val id: Int,
        val password: String,
        val salt: ByteArray
    )

    data class Request(
        val email: String,
        val password: String
    )
}
