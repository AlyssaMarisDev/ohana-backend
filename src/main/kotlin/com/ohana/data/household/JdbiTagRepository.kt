package com.ohana.data.household

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

    override fun findById(id: String): Tag? =
        handle
            .createQuery(
                """
            SELECT id, name, color, household_id, created_at, updated_at
            FROM tags
            WHERE id = :id
            """,
            ).bind("id", id)
            .mapTo(Tag::class.java)
            .findFirst()
            .orElse(null)

    override fun findByHouseholdId(householdId: String): List<Tag> =
        handle
            .createQuery(
                """
            SELECT id, name, color, household_id, created_at, updated_at
            FROM tags
            WHERE household_id = :householdId
            ORDER BY name
            """,
            ).bind("householdId", householdId)
            .mapTo(Tag::class.java)
            .list()

    override fun create(tag: Tag): Tag {
        handle
            .createUpdate(
                """
            INSERT INTO tags (id, name, color, household_id, created_at, updated_at)
            VALUES (:id, :name, :color, :householdId, :createdAt, :updatedAt)
            """,
            ).bind("id", tag.id)
            .bind("name", tag.name)
            .bind("color", tag.color)
            .bind("householdId", tag.householdId)
            .bind("createdAt", tag.createdAt)
            .bind("updatedAt", tag.updatedAt)
            .execute()

        return tag
    }

    override fun update(tag: Tag): Tag {
        handle
            .createUpdate(
                """
            UPDATE tags
            SET name = :name, color = :color, updated_at = :updatedAt
            WHERE id = :id
            """,
            ).bind("id", tag.id)
            .bind("name", tag.name)
            .bind("color", tag.color)
            .bind("updatedAt", tag.updatedAt)
            .execute()

        return tag
    }

    override fun deleteById(id: String): Boolean {
        val rowsAffected =
            handle
                .createUpdate("DELETE FROM tags WHERE id = :id")
                .bind("id", id)
                .execute()

        return rowsAffected > 0
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
