package com.ohana.domain.tags

import com.ohana.data.tags.TagPermission
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.domain.tags.TaskTagManager
import com.ohana.shared.enums.TagPermissionType

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
        val allTagIds = taskTagsMap.values.flatten().map { it.id }
        val filteredTags = filterTagsByPermissions(permission, allTagIds)

        // Filter the tasks that the user can view
        return taskIds.filter { taskId ->
            val taskTagIds = taskTagsMap[taskId]?.map { it.id } ?: emptyList()

            // Tasks with no tags are always viewable
            if (taskTagIds.isEmpty()) {
                return@filter true
            }

            // Task is viewable if it has at least one tag that the user can view
            taskTagIds.any { it in filteredTags }
        }
    }

    /**
     * Filters tags based on user permissions
     * Returns the list of tag IDs that the user is allowed to view
     */
    private fun filterTagsByPermissions(
        permission: TagPermission,
        allTagIds: List<String>,
    ): List<String> =
        when (permission.permissionType) {
            TagPermissionType.ALLOW_ALL_EXCEPT -> {
                // User can view all tags except those in the exception list
                allTagIds.filter { tagId -> !permission.tagIds.contains(tagId) }
            }
            TagPermissionType.DENY_ALL_EXCEPT -> {
                // User can only view tags in the exception list
                allTagIds.filter { tagId -> permission.tagIds.contains(tagId) }
            }
        }
}
