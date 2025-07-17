package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.NotFoundException

class TaskDeleteByIdHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    suspend fun handle(
        id: String,
        userId: String,
    ): Boolean =
        unitOfWork.execute { context ->
            // First, find the task to get its household ID
            val task = context.tasks.findById(id) ?: throw NotFoundException("Task not found")

            // Validate that the user is an active member of the household that the task belongs to
            householdMemberValidator.validate(context, task.householdId, userId)

            // Delete the task
            context.tasks.deleteById(id)
        }
}
