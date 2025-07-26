package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
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
        val dueDate: Instant?,
        val status: TaskStatus,
        val tagIds: List<String>,
    )

    data class Response(
        val id: String,
        val title: String,
        val description: String?,
        val dueDate: Instant?,
        val status: TaskStatus?,
        val completedAt: Instant?,
        val createdBy: String,
        val householdId: String,
        val tagIds: List<String>,
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

            // Determine the completed_at timestamp based on status change
            val completedAt =
                when {
                    request.status == TaskStatus.COMPLETED && existingTask.status != TaskStatus.COMPLETED -> Instant.now()
                    request.status != TaskStatus.COMPLETED -> null
                    else -> existingTask.completedAt
                }

            val updatedTask =
                context.tasks.update(
                    existingTask.copy(
                        title = request.title,
                        description = request.description,
                        dueDate = request.dueDate,
                        status = request.status,
                        completedAt = completedAt,
                    ),
                )

            val tags = taskTagManager.assignTagsToTask(context, updatedTask.id, request.tagIds)

            Response(
                id = updatedTask.id,
                title = updatedTask.title,
                description = updatedTask.description,
                dueDate = updatedTask.dueDate,
                status = updatedTask.status,
                completedAt = updatedTask.completedAt,
                createdBy = updatedTask.createdBy,
                householdId = updatedTask.householdId,
                tagIds = tags.map { it.id },
            )
        }
}
