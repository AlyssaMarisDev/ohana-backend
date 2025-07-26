package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException

class TaskGetByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
    private val taskTagManager: TaskTagManager,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant?,
        val status: TaskStatus,
        val completedAt: java.time.Instant?,
        val createdBy: String,
        val householdId: String,
        val tagIds: List<String>,
    )

    suspend fun handle(
        userId: String,
        id: String,
    ): Response =
        unitOfWork.execute { context ->
            val task = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            householdMemberValidator.validate(context, task.householdId, userId)

            val tags = taskTagManager.getTaskTags(context, task.id)

            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                completedAt = task.completedAt,
                createdBy = task.createdBy,
                householdId = task.householdId,
                tagIds = tags.map { it.id },
            )
        }
}
