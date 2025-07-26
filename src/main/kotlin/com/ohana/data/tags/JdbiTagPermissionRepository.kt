package com.ohana.data.tags

import com.ohana.shared.enums.TagPermissionType
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

private fun List<String>.toJsonString(): String = this.joinToString(",", "[", "]") { "\"$it\"" }

private fun String.fromJsonString(): List<String> =
    this
        .trim('[', ']')
        .split(",")
        .map { it.trim('"', ' ') }
        .filter { it.isNotEmpty() }

class JdbiTagPermissionRepository(
    private val handle: Handle,
) : TagPermissionRepository {
    private val rowMapper = TagPermissionRowMapper()

    override fun create(permission: TagPermission): TagPermission {
        val sql =
            """
            INSERT INTO household_member_tag_permissions (
                id, household_member_id, permission_type, tag_ids, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

        handle
            .createUpdate(sql)
            .bind(0, permission.id)
            .bind(1, permission.householdMemberId)
            .bind(2, permission.permissionType.name)
            .bind(3, permission.tagIds.toJsonString())
            .bind(4, permission.createdAt)
            .bind(5, permission.updatedAt)
            .execute()

        return permission
    }

    override fun update(permission: TagPermission): TagPermission {
        val sql =
            """
            UPDATE household_member_tag_permissions
            SET permission_type = ?, tag_ids = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent()

        val rowsAffected =
            handle
                .createUpdate(sql)
                .bind(0, permission.permissionType.name)
                .bind(1, permission.tagIds.toJsonString())
                .bind(2, permission.updatedAt)
                .bind(3, permission.id)
                .execute()

        if (rowsAffected == 0) {
            throw IllegalStateException("Tag permission with ID ${permission.id} not found")
        }

        return permission
    }

    override fun findById(id: String): TagPermission? {
        val sql = "SELECT * FROM household_member_tag_permissions WHERE id = ?"

        return handle
            .createQuery(sql)
            .bind(0, id)
            .map(rowMapper)
            .findFirst()
            .orElse(null)
    }

    override fun findByHouseholdMemberId(householdMemberId: String): TagPermission? {
        val sql = "SELECT * FROM household_member_tag_permissions WHERE household_member_id = ?"

        return handle
            .createQuery(sql)
            .bind(0, householdMemberId)
            .map(rowMapper)
            .findFirst()
            .orElse(null)
    }

    override fun deleteById(id: String): Boolean {
        val sql = "DELETE FROM household_member_tag_permissions WHERE id = ?"

        val rowsAffected =
            handle
                .createUpdate(sql)
                .bind(0, id)
                .execute()

        return rowsAffected > 0
    }

    override fun deleteByHouseholdMemberId(householdMemberId: String): Boolean {
        val sql = "DELETE FROM household_member_tag_permissions WHERE household_member_id = ?"

        val rowsAffected =
            handle
                .createUpdate(sql)
                .bind(0, householdMemberId)
                .execute()

        return rowsAffected > 0
    }

    private class TagPermissionRowMapper : RowMapper<TagPermission> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): TagPermission =
            TagPermission(
                id = rs.getString("id"),
                householdMemberId = rs.getString("household_member_id"),
                permissionType = TagPermissionType.valueOf(rs.getString("permission_type")),
                tagIds = rs.getString("tag_ids").fromJsonString(),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
    }
}
