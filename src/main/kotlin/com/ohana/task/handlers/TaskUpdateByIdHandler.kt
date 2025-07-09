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
            val task = fetchTaskById(handle, id)
            if (task == null) {
                throw NotFoundException("Task not found")
            }

            val updatedRows = updateTask(handle, id, request)

            if (updatedRows == 0) {
                throw DbException("Failed to update task")
            }

            fetchTaskById(handle, id)
        }

    private fun updateTask(
        handle: Handle,
        id: String,
        request: Request,
    ): Int {
        val updateQuery = """
            UPDATE tasks
            SET title = :title,
                description = :description,
                dueDate = :dueDate,
                status = :status
            WHERE id = :id
        """

        return update(
            handle,
            updateQuery,
            mapOf(
                "id" to id,
                "title" to request.title,
                "description" to request.description,
                "dueDate" to request.dueDate,
                "status" to request.status?.name,
            ),
        )
    }

    private fun fetchTaskById(
        handle: Handle,
        id: String,
    ): Response {
        val selectQuery = """
            SELECT id, title, description, dueDate, status, createdBy
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
}
