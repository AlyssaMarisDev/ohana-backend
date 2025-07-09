package com.ohana.tasks.handlers

import com.ohana.shared.TaskStatus
import com.ohana.utils.DatabaseUtils.Companion.get
import com.ohana.utils.DatabaseUtils.Companion.query
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.time.Instant

class TasksGetAllHandler(
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

    suspend fun handle(): List<Response> =
        query(jdbi) { handle ->
            getTasks(handle)
        }

    fun getTasks(handle: Handle): List<Response> =
        get(
            handle,
            "SELECT id, title, description, dueDate, status, createdBy FROM tasks",
            mapOf(),
            Response::class,
        )
}
