package com.ohana.data.task

import com.ohana.data.utils.DatabaseUtils
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle

class JdbiTaskRepository(
    private val handle: Handle,
) : TaskRepository {
    override fun create(task: Task): Task {
        val insertQuery = """
            INSERT INTO tasks (id, title, description, due_date, status, created_by, household_id)
            VALUES (:id, :title, :description, :due_date, :status, :created_by, :household_id)
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
                    "created_by" to task.createdBy,
                    "household_id" to task.householdId,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create task")

        return findById(task.id) ?: throw NotFoundException("Task not found after creation")
    }

    override fun findById(id: String): Task? {
        val selectQuery = """
            SELECT id, title, description, due_date, status, created_by, household_id
            FROM tasks
            WHERE id = :id
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("id" to id),
                Task.mapper,
            ).firstOrNull()
    }

    override fun findAll(): List<Task> {
        val selectQuery = """
            SELECT id, title, description, due_date, status, created_by, household_id
            FROM tasks
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf(),
                Task.mapper,
            )
    }

    override fun findByHouseholdId(householdId: String): List<Task> {
        val selectQuery = """
            SELECT id, title, description, due_date, status, created_by, household_id
            FROM tasks
            WHERE household_id = :household_id
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("household_id" to householdId),
                Task.mapper,
            )
    }

    override fun findByHouseholdIds(householdIds: List<String>): List<Task> {
        if (householdIds.isEmpty()) {
            return emptyList()
        }

        val placeholders = householdIds.mapIndexed { index, _ -> ":household_id_$index" }.joinToString(", ")
        val selectQuery = """
            SELECT id, title, description, due_date, status, created_by, household_id
            FROM tasks
            WHERE household_id IN ($placeholders)
            ORDER BY created_at DESC
        """

        val params = householdIds.mapIndexed { index, id -> "household_id_$index" to id }.toMap()

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                params,
                Task.mapper,
            )
    }

    override fun update(task: Task): Task {
        val updateQuery = """
            UPDATE tasks
            SET title = :title, description = :description, due_date = :due_date, status = :status
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
}
