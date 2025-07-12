package com.ohana.tasks.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.UnitOfWork

class TaskGetByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant,
        val status: com.ohana.shared.TaskStatus,
        val createdBy: String,
    )

    suspend fun handle(id: String): Response =
        unitOfWork.execute { context ->
            val task = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                createdBy = task.createdBy,
            )
        }
}
