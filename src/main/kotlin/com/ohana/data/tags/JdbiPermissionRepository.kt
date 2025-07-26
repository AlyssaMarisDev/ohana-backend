package com.ohana.data.tags

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiPermissionRepository(
    private val handle: Handle,
) : PermissionRepository {
    private val rowMapper = PermissionRowMapper()

    override fun findByHouseholdMemberId(householdMemberId: String): Permission? {
        val sql = "SELECT * FROM permissions WHERE household_member_id = ?"

        return handle
            .createQuery(sql)
            .bind(0, householdMemberId)
            .map(rowMapper)
            .findFirst()
            .orElse(null)
    }

    private class PermissionRowMapper : RowMapper<Permission> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Permission =
            Permission(
                id = rs.getString("id"),
                householdMemberId = rs.getString("household_member_id"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
    }
}
