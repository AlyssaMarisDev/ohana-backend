package com.ohana.task.handlers

import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.task.entities.Task
import org.slf4j.LoggerFactory
import java.time.Instant

class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
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
        unitOfWork.execute { context ->
            // Validate that the user exists
            context.members.findById(userId)
                ?: throw IllegalArgumentException("User not found")

            // Create the task
            val task =
                context.tasks.create(
                    Task(
                        id = request.id,
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                        createdBy = userId,
                    ),
                )

            // Convert to response
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
