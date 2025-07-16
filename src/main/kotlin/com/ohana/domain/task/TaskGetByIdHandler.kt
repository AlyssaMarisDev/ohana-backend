package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import com.ohana.shared.exceptions.NotFoundException

class TaskGetByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant,
        val status: TaskStatus,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        id: String,
        userId: String,
    ): Response =
        unitOfWork.execute { context ->
            // First, find the task to get its household ID
            val task = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            // Validate that the user is an active member of the household that the task belongs to
            householdMemberValidator.validate(context, task.householdId, userId)

            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                createdBy = task.createdBy,
                householdId = task.householdId,
            )
        }
}
