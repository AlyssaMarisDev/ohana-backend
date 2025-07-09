package com.ohana.tasks.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.TaskStatus
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.insert
import com.ohana.utils.DatabaseUtils.Companion.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

class TaskCreationHandler(
    private val jdbi: Jdbi,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    data class Request(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
        val createdBy: String,
    )

    suspend fun handle(
        userId: String,
        request: Request,
    ): Response =
        transaction(jdbi) { handle ->
            val insertedRows = insertTask(handle, userId, request)

            if (insertedRows == 0) {
                throw Exception("Failed to create task")
            }

            getTaskById(handle, request.id)
        }

    private fun insertTask(
        handle: Handle,
        userId: String,
        request: Request,
    ): Int {
        val insertQuery = """
            INSERT INTO tasks (id, title, description, dueDate, status, createdBy)
            VALUES (:id, :title, :description, :dueDate, :status, :createdBy)
        """

        return insert(
            handle,
            insertQuery,
            mapOf(
                "id" to request.id,
                "title" to request.title,
                "description" to request.description,
                "dueDate" to request.dueDate,
                "status" to request.status.name,
                "createdBy" to userId,
            ),
        )
    }

    private fun getTaskById(
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
