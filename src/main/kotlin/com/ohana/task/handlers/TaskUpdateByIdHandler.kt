package com.ohana.tasks.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.UnitOfWork
import java.time.Instant

class TaskUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: com.ohana.shared.TaskStatus?,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: com.ohana.shared.TaskStatus?,
        val createdBy: String,
    )

    suspend fun handle(
        id: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Get existing task
            val existingTask = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            // Update task
            val updatedTask =
                context.tasks.update(
                    existingTask.copy(
                        title = request.title,
                        description = request.description ?: existingTask.description,
                        dueDate = request.dueDate,
                        status = request.status ?: existingTask.status,
                    ),
                )

            // Return response
            Response(
                id = updatedTask.id,
                title = updatedTask.title,
                description = updatedTask.description,
                dueDate = updatedTask.dueDate,
                status = updatedTask.status,
                createdBy = updatedTask.createdBy,
            )
        }
}
