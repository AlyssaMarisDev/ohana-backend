package com.ohana.data.tags

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiTagPermissionRepository(
    private val handle: Handle,
) : TagPermissionRepository {
    private val rowMapper = TagPermissionRowMapper()

    override fun findByPermissionId(permissionId: String): List<TagPermission> {
        val sql = "SELECT * FROM tag_permissions WHERE permission_id = ?"

        return handle
            .createQuery(sql)
            .bind(0, permissionId)
            .map(rowMapper)
            .list()
    }

    private class TagPermissionRowMapper : RowMapper<TagPermission> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): TagPermission =
            TagPermission(
                id = rs.getString("id"),
                permissionId = rs.getString("permission_id"),
                tagId = rs.getString("tag_id"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
    }
}
