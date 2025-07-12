package com.ohana.task.handlers

import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.UnitOfWork

class TaskGetAllHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
) {
    data class Response(
        val id: String,
        val title: String,
        val description: String,
        val dueDate: java.time.Instant,
        val status: com.ohana.shared.TaskStatus,
        val createdBy: String,
        val householdId: String,
    )

    suspend fun handle(
        householdId: String,
        userId: String,
    ): List<Response> =
        unitOfWork.execute { context ->
            householdMemberValidator.validate(context, householdId, userId)

            context.tasks.findByHouseholdId(householdId).map { task ->
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
}
