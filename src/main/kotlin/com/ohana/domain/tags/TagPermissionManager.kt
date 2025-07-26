package com.ohana.domain.tags

import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.domain.tags.TaskTagManager

class TagPermissionManager(
    private val taskTagManager: TaskTagManager,
) {
    /**
     * Filters tasks based on user's tag permissions
     * Returns only tasks that have at least one tag the user can view
     */
    fun filterTasksByTagPermissions(
        context: UnitOfWorkContext,
        householdMemberId: String,
        taskIds: List<String>,
    ): List<String> {
        if (taskIds.isEmpty()) {
            return emptyList()
        }

        val permission = context.tagPermissions.findByHouseholdMemberId(householdMemberId)

        // If no permission is set, user can view all tasks (default behavior)
        if (permission == null) {
            return taskIds
        }

        // Get all task tags for the given tasks
        val taskTagsMap = taskTagManager.getTasksTags(context, taskIds)

        // Get the tags that the user can view
        val userCanViewTags = permission.tagIds.toSet()

        // Filter the tasks that the user can view
        return taskIds.filter { taskId ->
            val taskTagIds = taskTagsMap[taskId]?.map { it.id } ?: emptyList()

            // Tasks with no tags are always viewable
            if (taskTagIds.isEmpty()) {
                return@filter true
            }

            // Task is viewable if it has at least one tag that the user can view
            taskTagIds.any { it in userCanViewTags }
        }
    }
}
