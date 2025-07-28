package com.ohana.data.permissions

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiPermissionRepository(
    private val handle: Handle,
) : PermissionRepository {
    private val rowMapper = PermissionRowMapper()

    override fun findByHouseholdMemberId(householdMemberId: String): Permission? {
        val sql = "SELECT * FROM permissions WHERE household_member_id = :householdMemberId"

        return handle
            .createQuery(sql)
            .bind("householdMemberId", householdMemberId)
            .map(rowMapper)
            .findFirst()
            .orElse(null)
    }

    override fun create(permission: Permission): Permission {
        val insertQuery = """
            INSERT INTO permissions (id, household_member_id, created_at, updated_at)
            VALUES (:id, :householdMemberId, :createdAt, :updatedAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to permission.id,
                    "householdMemberId" to permission.householdMemberId,
                    "createdAt" to permission.createdAt,
                    "updatedAt" to permission.updatedAt,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create permission")

        return findByHouseholdMemberId(permission.householdMemberId)
            ?: throw NotFoundException("Permission not found after creation")
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
