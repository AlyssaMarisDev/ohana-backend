package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.enums.TaskStatus

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
        householdId: String,
        userId: String,
    ): Response =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, householdId, userId)

            val task = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            // Validate that the task belongs to the specified household
            if (task.householdId != householdId) {
                throw NotFoundException("Task not found in this household")
            }

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
