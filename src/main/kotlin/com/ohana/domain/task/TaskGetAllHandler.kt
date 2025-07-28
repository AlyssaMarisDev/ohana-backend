package com.ohana.domain.task

import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.enums.TaskStatus
import java.time.Instant

class TaskGetAllHandler(
    private val unitOfWork: UnitOfWork,
    private val householdMemberValidator: HouseholdMemberValidator,
    private val taskTagManager: TaskTagManager,
    private val tagPermissionManager: TagPermissionManager,
) {
    data class Request(
        val householdIds: List<String>,
        val dueDateFrom: Instant?,
        val dueDateTo: Instant?,
        val completedDateFrom: Instant?,
        val completedDateTo: Instant?,
    )

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
        request: Request,
    ): List<Response> =
        unitOfWork.execute { context ->
            val effectiveHouseholdIds = getEffectiveHouseholdIds(context, request.householdIds, userId)

            val tasks =
                context.tasks.findByHouseholdIdsWithDateFilters(
                    householdIds = effectiveHouseholdIds,
                    dueDateFrom = request.dueDateFrom,
                    dueDateTo = request.dueDateTo,
                    completedDateFrom = request.completedDateFrom,
                    completedDateTo = request.completedDateTo,
                )
            val taskIds = tasks.map { it.id }

            // Filter tasks based on tag permissions for each household
            val filteredTaskIds = mutableListOf<String>()
            for (householdId in effectiveHouseholdIds) {
                val householdMember = context.households.findMemberById(householdId, userId)
                if (householdMember != null) {
                    val householdTaskIds = tasks.filter { it.householdId == householdId }.map { it.id }
                    val filteredHouseholdTaskIds =
                        tagPermissionManager.filterTasksByTagPermissions(
                            context,
                            householdMember.id,
                            householdTaskIds,
                        )
                    filteredTaskIds.addAll(filteredHouseholdTaskIds)
                }
            }

            // Get only the filtered tasks
            val filteredTasks = tasks.filter { it.id in filteredTaskIds }
            val filteredTaskIdsForTags = filteredTasks.map { it.id }

            val tags = taskTagManager.getTasksTags(context, filteredTaskIdsForTags)

            filteredTasks.map { task ->
                Response(
                    id = task.id,
                    title = task.title,
                    description = task.description,
                    dueDate = task.dueDate,
                    status = task.status,
                    completedAt = task.completedAt,
                    createdBy = task.createdBy,
                    householdId = task.householdId,
                    tagIds = tags[task.id]?.map { it.id } ?: emptyList(),
                )
            }
        }

    private fun getEffectiveHouseholdIds(
        context: UnitOfWorkContext,
        householdIds: List<String>,
        userId: String,
    ): List<String> {
        if (householdIds.isEmpty()) {
            return context.households.findByMemberId(userId).map { it.id }
        } else {
            householdIds.forEach { householdId ->
                householdMemberValidator.validate(context, householdId, userId)
            }
            return householdIds
        }
    }
}
