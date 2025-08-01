package com.ohana.data.task

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

class JdbiTaskRepository(
    private val handle: Handle,
) : TaskRepository {
    init {
        handle.registerRowMapper(TaskRowMapper())
    }

    override fun create(task: Task): Task {
        val insertQuery = """
            INSERT INTO tasks (id, title, description, due_date, status, completed_at, created_by, household_id)
            VALUES (:id, :title, :description, :due_date, :status, :completed_at, :created_by, :household_id)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "description" to task.description,
                    "due_date" to task.dueDate,
                    "status" to task.status.name,
                    "completed_at" to task.completedAt,
                    "created_by" to task.createdBy,
                    "household_id" to task.householdId,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create task")

        return findById(task.id) ?: throw NotFoundException("Task not found after creation")
    }

    override fun findById(id: String): Task? {
        val selectQuery = """
            SELECT id, title, description, due_date, status, completed_at, created_by, household_id
            FROM tasks
            WHERE id = :id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("id" to id),
                Task::class,
            ).firstOrNull()
    }

    override fun findAll(): List<Task> {
        val selectQuery = """
            SELECT id, title, description, due_date, status, completed_at, created_by, household_id
            FROM tasks
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf(),
                Task::class,
            )
    }

    override fun findByHouseholdId(householdId: String): List<Task> {
        val selectQuery = """
            SELECT id, title, description, due_date, status, completed_at, created_by, household_id
            FROM tasks
            WHERE household_id = :household_id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("household_id" to householdId),
                Task::class,
            )
    }

    override fun findByHouseholdIds(householdIds: List<String>): List<Task> {
        if (householdIds.isEmpty()) {
            return emptyList()
        }

        val placeholders = householdIds.mapIndexed { index, _ -> ":household_id_$index" }.joinToString(", ")
        val selectQuery = """
            SELECT id, title, description, due_date, status, completed_at, created_by, household_id
            FROM tasks
            WHERE household_id IN ($placeholders)
            ORDER BY created_at DESC
        """

        val params = householdIds.mapIndexed { index, id -> "household_id_$index" to id }.toMap()

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                params,
                Task::class,
            )
    }

    override fun findByHouseholdIdsWithDateFilters(
        householdIds: List<String>,
        dueDateFrom: Instant?,
        dueDateTo: Instant?,
        completedDateFrom: Instant?,
        completedDateTo: Instant?,
    ): List<Task> {
        if (householdIds.isEmpty()) {
            return emptyList()
        }

        val placeholders = householdIds.mapIndexed { index, _ -> ":household_id_$index" }.joinToString(", ")

        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        // Add household IDs condition
        conditions.add("household_id IN ($placeholders)")
        householdIds.forEachIndexed { index, id ->
            params["household_id_$index"] = id
        }

        // Add due date range conditions
        if (dueDateFrom != null) {
            conditions.add("due_date >= :due_date_from")
            params["due_date_from"] = dueDateFrom
        }
        if (dueDateTo != null) {
            conditions.add("due_date <= :due_date_to")
            params["due_date_to"] = dueDateTo
        }

        // Add completed date range conditions
        if (completedDateFrom != null) {
            conditions.add("completed_at >= :completed_date_from")
            params["completed_date_from"] = completedDateFrom
        }
        if (completedDateTo != null) {
            conditions.add("completed_at <= :completed_date_to")
            params["completed_date_to"] = completedDateTo
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""

        val selectQuery = """
            SELECT id, title, description, due_date, status, completed_at, created_by, household_id
            FROM tasks
            $whereClause
            ORDER BY created_at DESC
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                params,
                Task::class,
            )
    }

    override fun update(task: Task): Task {
        val updateQuery = """
            UPDATE tasks
            SET title = :title, description = :description, due_date = :due_date, status = :status, completed_at = :completed_at
            WHERE id = :id
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "description" to task.description,
                    "due_date" to task.dueDate,
                    "status" to task.status.name,
                    "completed_at" to task.completedAt,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update task")

        return findById(task.id) ?: throw NotFoundException("Task not found after update")
    }

    override fun deleteById(id: String): Boolean {
        val deleteQuery = """
            DELETE FROM tasks
            WHERE id = :id
        """

        val deletedRows =
            DatabaseUtils.delete(
                handle,
                deleteQuery,
                mapOf("id" to id),
            )

        return deletedRows > 0
    }

    private class TaskRowMapper : RowMapper<Task> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Task =
            Task(
                id = rs.getString("id"),
                title = rs.getString("title"),
                description = rs.getString("description"),
                dueDate = rs.getTimestamp("due_date")?.toInstant(),
                status =
                    com.ohana.shared.enums.TaskStatus
                        .valueOf(rs.getString("status").uppercase()),
                completedAt = rs.getTimestamp("completed_at")?.toInstant(),
                createdBy = rs.getString("created_by"),
                householdId = rs.getString("household_id"),
            )
    }
}
