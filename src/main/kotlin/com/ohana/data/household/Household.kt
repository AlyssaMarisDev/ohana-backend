package com.ohana.data.household

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

data class Household(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
) {
    companion object {
        /**
         * Maps a database row to a Household object
         */
        val mapper: RowMapper<Household> =
            RowMapper { rs: ResultSet, _: StatementContext ->
                Household(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    description = rs.getString("description"),
                    createdBy = rs.getString("created_by"),
                )
            }
    }
}
