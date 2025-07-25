package com.ohana.data.task

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class JdbiTaskTagRepository(
    private val handle: Handle,
) : TaskTagRepository {
    init {
        handle.registerRowMapper(TaskTagRowMapper())
    }

    override fun findByTaskId(taskId: String): List<TaskTag> =
        handle
            .createQuery(
                """
                SELECT id, task_id, tag_id, created_at
                FROM task_tags
                WHERE task_id = :taskId
                ORDER BY created_at
                """,
            ).bind("taskId", taskId)
            .mapTo(TaskTag::class.java)
            .list()

    override fun findByTaskIds(taskIds: List<String>): List<TaskTag> =
        if (taskIds.isEmpty()) {
            emptyList()
        } else {
            handle
                .createQuery(
                    """
                    SELECT id, task_id, tag_id, created_at
                    FROM task_tags
                    WHERE task_id IN (<taskIds>)
                    ORDER BY task_id, created_at
                    """,
                ).bindList("taskIds", taskIds)
                .mapTo(TaskTag::class.java)
                .list()
        }

    override fun create(taskTag: TaskTag): TaskTag {
        handle
            .createUpdate(
                """
                INSERT INTO task_tags (id, task_id, tag_id, created_at)
                VALUES (:id, :taskId, :tagId, :createdAt)
                """,
            ).bind("id", taskTag.id)
            .bind("taskId", taskTag.taskId)
            .bind("tagId", taskTag.tagId)
            .bind("createdAt", taskTag.createdAt)
            .execute()

        return taskTag
    }

    override fun createMany(taskTags: List<TaskTag>): List<TaskTag> {
        if (taskTags.isEmpty()) {
            return emptyList()
        }

        val batch =
            handle.prepareBatch(
                """
            INSERT INTO task_tags (id, task_id, tag_id, created_at)
            VALUES (:id, :taskId, :tagId, :createdAt)
            """,
            )

        taskTags.forEach { taskTag ->
            batch
                .bind("id", taskTag.id)
                .bind("taskId", taskTag.taskId)
                .bind("tagId", taskTag.tagId)
                .bind("createdAt", taskTag.createdAt)
                .add()
        }

        batch.execute()

        return taskTags
    }

    override fun deleteByTaskId(taskId: String): Boolean {
        val rowsAffected =
            handle
                .createUpdate("DELETE FROM task_tags WHERE task_id = :taskId")
                .bind("taskId", taskId)
                .execute()

        return rowsAffected > 0
    }

    override fun deleteByTaskIdAndTagIds(
        taskId: String,
        tagIds: List<String>,
    ): Boolean {
        if (tagIds.isEmpty()) {
            return false
        }

        val rowsAffected =
            handle
                .createUpdate(
                    """
                    DELETE FROM task_tags
                    WHERE task_id = :taskId AND tag_id IN (<tagIds>)
                    """,
                ).bind("taskId", taskId)
                .bindList("tagIds", tagIds)
                .execute()

        return rowsAffected > 0
    }

    private class TaskTagRowMapper : RowMapper<TaskTag> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): TaskTag =
            TaskTag(
                id = rs.getString("id"),
                taskId = rs.getString("task_id"),
                tagId = rs.getString("tag_id"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            )
    }
}
