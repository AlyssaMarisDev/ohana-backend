package com.ohana.data.member

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

data class Member(
    val id: String,
    val name: String,
    val email: String,
    val age: Int? = null,
    val gender: String? = null,
) {
    companion object {
        /**
         * Maps a database row to a Member object
         */
        val mapper: RowMapper<Member> =
            RowMapper { rs: ResultSet, _: StatementContext ->
                Member(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    age = rs.getObject("age") as? Int,
                    gender = rs.getString("gender"),
                )
            }
    }
}
