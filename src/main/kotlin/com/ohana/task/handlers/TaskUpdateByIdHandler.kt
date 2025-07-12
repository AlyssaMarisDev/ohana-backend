package com.ohana.tasks.handlers

import com.ohana.exceptions.DbException
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.TaskStatus
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.transaction
import com.ohana.utils.DatabaseUtils.Companion.update
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.time.Instant

class TaskUpdateByIdHandler(
    private val jdbi: Jdbi,
) {
    data class Request(
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: TaskStatus?,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: TaskStatus?,
        val createdBy: String,
    )

    suspend fun handle(
        id: String,
        request: Request,
    ): Response =
        transaction(jdbi) { handle ->
            validateTaskExists(handle, id)
            updateTask(handle, id, request)
            getTaskById(handle, id)
        }

    private fun updateTask(
        handle: Handle,
        id: String,
        request: Request,
    ) {
        val updateQuery = """
            UPDATE tasks
            SET title = :title,
                description = :description,
                due_date = :due_date,
                status = :status
            WHERE id = :id
        """

        val updatedRows =
            update(
                handle,
                updateQuery,
                mapOf(
                    "id" to id,
                    "title" to request.title,
                    "description" to request.description,
                    "due_date" to request.dueDate,
                    "status" to request.status?.name,
                ),
            )

        if (updatedRows == 0) throw DbException("Failed to update task")
    }

    private fun getTaskById(
        handle: Handle,
        id: String,
    ): Response {
        val selectQuery = """
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy
            FROM tasks
            WHERE id = :id
        """

        return get(
            handle,
            selectQuery,
            mapOf("id" to id),
            Response::class,
        ).firstOrNull() ?: throw NotFoundException("Task not found")
    }

    private fun validateTaskExists(
        handle: Handle,
        id: String,
    ) {
        val taskId = getIfTaskExists(handle, id)
        if (taskId == null) {
            throw NotFoundException("Task not found")
        }
    }

    private fun getIfTaskExists(
        handle: Handle,
        id: String,
    ): String? {
        val selectQuery = """
            SELECT id
            FROM tasks
            WHERE id = :id
        """

        return get(
            handle,
            selectQuery,
            mapOf("id" to id),
            String::class,
        ).firstOrNull()
    }
}
