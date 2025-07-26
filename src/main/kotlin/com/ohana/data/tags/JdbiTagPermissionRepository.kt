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

    override fun findByHouseholdMemberId(householdMemberId: String): TagPermission? {
        val sql = "SELECT * FROM household_member_tag_permissions WHERE household_member_id = ?"

        return handle
            .createQuery(sql)
            .bind(0, householdMemberId)
            .map(rowMapper)
            .findFirst()
            .orElse(null)
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
