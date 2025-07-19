package com.ohana.data.auth

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

data class AuthMember(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val salt: ByteArray,
    val age: Int? = null,
    val gender: String? = null,
) {
    companion object {
        /**
         * Maps a database row to an AuthMember object
         */
        val mapper: RowMapper<AuthMember> =
            RowMapper { rs: ResultSet, _: StatementContext ->
                AuthMember(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    password = rs.getString("password"),
                    salt = rs.getBytes("salt"),
                    age = rs.getObject("age") as? Int,
                    gender = rs.getString("gender"),
                )
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthMember

        if (id != other.id) return false
        if (name != other.name) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (!salt.contentEquals(other.salt)) return false
        if (age != other.age) return false
        if (gender != other.gender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + (age ?: 0)
        result = 31 * result + (gender?.hashCode() ?: 0)
        return result
    }
}
