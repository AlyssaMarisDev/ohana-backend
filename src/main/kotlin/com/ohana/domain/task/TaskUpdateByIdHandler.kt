package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.*
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException
import java.time.Instant

class TaskUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
    private val taskTagManager: TaskTagManager,
) {
    data class Request(
        val title: String,
        val description: String,
        val dueDate: Instant,
        val status: TaskStatus,
        val tagIds: List<String>,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant,
        val status: TaskStatus?,
        val createdBy: String,
        val householdId: String,
        val tags: List<TaskTagResponse>,
    )

    data class TaskTagResponse(
        val id: String,
        val name: String,
        val color: String,
    )

    suspend fun handle(
        userId: String,
        id: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            val existingTask = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            // Validate that the user is a member of the household that the task belongs to
            householdMemberValidator.validate(context, existingTask.householdId, userId)

            val updatedTask =
                context.tasks.update(
                    existingTask.copy(
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                    ),
                )

            val tags = taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)

            Response(
                id = updatedTask.id,
                title = updatedTask.title,
                description = updatedTask.description,
                dueDate = updatedTask.dueDate,
                status = updatedTask.status,
                createdBy = updatedTask.createdBy,
                householdId = updatedTask.householdId,
                tags =
                    tags.map {
                        TaskTagResponse(
                            id = it.id,
                            name = it.name,
                            color = it.color,
                        )
                    },
            )
        }
}
