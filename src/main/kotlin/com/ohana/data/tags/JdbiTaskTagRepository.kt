package com.ohana.data.tags

import com.ohana.data.utils.DatabaseUtils
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

    override fun findByTaskId(taskId: String): List<TaskTag> {
        val selectQuery = """
            SELECT id, task_id, tag_id, created_at
            FROM task_tags
            WHERE task_id = :taskId
            ORDER BY created_at
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("taskId" to taskId),
                TaskTag::class,
            )
    }

    override fun findByTaskIds(taskIds: List<String>): List<TaskTag> {
        if (taskIds.isEmpty()) {
            return emptyList()
        }

        val placeholders = taskIds.mapIndexed { index, _ -> ":taskId_$index" }.joinToString(", ")
        val selectQuery = """
            SELECT id, task_id, tag_id, created_at
            FROM task_tags
            WHERE task_id IN ($placeholders)
            ORDER BY task_id, created_at
        """

        val params = taskIds.mapIndexed { index, id -> "taskId_$index" to id }.toMap()

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                params,
                TaskTag::class,
            )
    }

    override fun create(taskTag: TaskTag): TaskTag {
        val insertQuery = """
            INSERT INTO task_tags (id, task_id, tag_id, created_at)
            VALUES (:id, :taskId, :tagId, :createdAt)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to taskTag.id,
                    "taskId" to taskTag.taskId,
                    "tagId" to taskTag.tagId,
                    "createdAt" to taskTag.createdAt,
                ),
            )

        if (insertedRows == 0) {
            throw com.ohana.shared.exceptions
                .DbException("Failed to create task tag")
        }

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
        val deleteQuery = """
            DELETE FROM task_tags WHERE task_id = :taskId
        """

        val deletedRows =
            DatabaseUtils.delete(
                handle,
                deleteQuery,
                mapOf("taskId" to taskId),
            )

        return deletedRows > 0
    }

    override fun deleteByTaskIdAndTagIds(
        taskId: String,
        tagIds: List<String>,
    ): Boolean {
        if (tagIds.isEmpty()) {
            return false
        }

        val placeholders = tagIds.mapIndexed { index, _ -> ":tagId_$index" }.joinToString(", ")
        val deleteQuery = """
            DELETE FROM task_tags
            WHERE task_id = :taskId AND tag_id IN ($placeholders)
        """

        val params = mapOf("taskId" to taskId) + tagIds.mapIndexed { index, id -> "tagId_$index" to id }.toMap()

        val deletedRows =
            DatabaseUtils.delete(
                handle,
                deleteQuery,
                params,
            )

        return deletedRows > 0
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
