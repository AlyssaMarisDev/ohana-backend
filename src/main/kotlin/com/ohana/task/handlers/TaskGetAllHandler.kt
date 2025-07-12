package com.ohana.tasks.handlers

import com.ohana.shared.UnitOfWork

class TaskGetAllHandler(
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

    suspend fun handle(): List<Response> =
        unitOfWork.execute { context ->
            context.tasks.findAll().map { task ->
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
}
