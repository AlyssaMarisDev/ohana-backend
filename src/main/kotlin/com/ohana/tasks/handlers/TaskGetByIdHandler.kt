package com.ohana.tasks.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.TaskStatus
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.time.Instant

class TaskGetByIdHandler(
    private val jdbi: Jdbi,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
        val createdBy: String,
    )

    suspend fun handle(id: String): Response =
        query(jdbi) { handle ->
            getTaskById(handle, id) ?: throw NotFoundException("Task not found")
        }

    fun getTaskById(
        handle: Handle,
        id: String,
    ): Response? =
        get(
            handle,
            "SELECT id, title, description, dueDate, status, createdBy FROM tasks WHERE id = :id",
            mapOf("id" to id),
            Response::class,
        ).firstOrNull()
}
