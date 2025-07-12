package com.ohana.task.repositories

import com.ohana.exceptions.DbException
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.TaskRepository
import com.ohana.task.entities.Task
import com.ohana.utils.DatabaseUtils
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
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy, household_id as householdId
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
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy, household_id as householdId
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
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy, household_id as householdId
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
}
