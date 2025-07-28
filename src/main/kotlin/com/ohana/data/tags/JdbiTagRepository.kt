package com.ohana.data.tags

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiTagRepository(
    private val handle: Handle,
) : TagRepository {
    init {
        handle.registerRowMapper(TagRowMapper())
    }

    override fun findById(id: String): Tag? {
        val selectQuery = """
            SELECT id, name, color, household_id, created_at, updated_at
            FROM tags
            WHERE id = :id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("id" to id),
                Tag::class,
            ).firstOrNull()
    }

    override fun findByIds(ids: List<String>): List<Tag> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val placeholders = ids.mapIndexed { index, _ -> ":id_$index" }.joinToString(", ")
        val selectQuery = """
            SELECT id, name, color, household_id, created_at, updated_at
            FROM tags
            WHERE id IN ($placeholders)
        """

        val params = ids.mapIndexed { index, id -> "id_$index" to id }.toMap()

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                params,
                Tag::class,
            )
    }

    override fun findByHouseholdId(householdId: String): List<Tag> {
        val selectQuery = """
            SELECT id, name, color, household_id, created_at, updated_at
            FROM tags
            WHERE household_id = :householdId
            ORDER BY name
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("householdId" to householdId),
                Tag::class,
            )
    }

    override fun create(tag: Tag): Tag {
        val insertQuery = """
            INSERT INTO tags (id, name, color, household_id, created_at, updated_at)
            VALUES (:id, :name, :color, :householdId, :createdAt, :updatedAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to tag.id,
                    "name" to tag.name,
                    "color" to tag.color,
                    "householdId" to tag.householdId,
                    "createdAt" to tag.createdAt,
                    "updatedAt" to tag.updatedAt,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create tag")

        return findById(tag.id) ?: throw NotFoundException("Tag not found after creation")
    }

    override fun update(tag: Tag): Tag {
        val updateQuery = """
            UPDATE tags
            SET name = :name, color = :color, updated_at = :updatedAt
            WHERE id = :id
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "id" to tag.id,
                    "name" to tag.name,
                    "color" to tag.color,
                    "updatedAt" to tag.updatedAt,
                ),
            )

        if (updatedRows == 0) throw NotFoundException("Tag not found for update")

        return findById(tag.id) ?: throw NotFoundException("Tag not found after update")
    }

    override fun deleteById(id: String): Boolean {
        val deleteQuery = "DELETE FROM tags WHERE id = :id"

        val deletedRows =
            DatabaseUtils.delete(
                handle,
                deleteQuery,
                mapOf("id" to id),
            )

        return deletedRows > 0
    }

    private class TagRowMapper : RowMapper<Tag> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Tag =
            Tag(
                id = rs.getString("id"),
                name = rs.getString("name"),
                color = rs.getString("color"),
                householdId = rs.getString("household_id"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
    }
}
