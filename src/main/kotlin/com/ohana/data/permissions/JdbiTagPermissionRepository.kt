package com.ohana.data.permissions

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiTagPermissionRepository(
    private val handle: Handle,
) : TagPermissionRepository {
    private val rowMapper = TagPermissionRowMapper()

    override fun findByPermissionId(permissionId: String): List<TagPermission> {
        val sql = "SELECT * FROM tag_permissions WHERE permission_id = :permissionId"

        return handle
            .createQuery(sql)
            .bind("permissionId", permissionId)
            .map(rowMapper)
            .list()
    }

    override fun create(tagPermission: TagPermission): TagPermission {
        val insertQuery = """
            INSERT INTO tag_permissions (id, permission_id, tag_id, created_at)
            VALUES (:id, :permissionId, :tagId, :createdAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to tagPermission.id,
                    "permissionId" to tagPermission.permissionId,
                    "tagId" to tagPermission.tagId,
                    "createdAt" to tagPermission.createdAt,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create tag permission")

        return tagPermission
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
