package com.ohana.domain.tags

import com.ohana.data.tags.Tag
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

        val permission = context.permissions.findByHouseholdMemberId(householdMemberId)

        // If no permission is set, user can't view any tasks
        if (permission == null) {
            return emptyList()
        }

        // Get the tags that the user can view
        val userViewableTagIds = getUserViewableTagIds(context, permission.id)

        // Get all task tags for the given tasks
        val taskTagsMap = taskTagManager.getTasksTags(context, taskIds)

        // Filter the tasks that the user can view
        return taskIds.filter { taskId ->
            val taskTagIds = taskTagsMap[taskId]?.map { it.id } ?: emptyList()

            // Tasks with no tags are always viewable
            if (taskTagIds.isEmpty()) {
                return@filter true
            }

            // Task is viewable if it has at least one tag that the user can view
            taskTagIds.any { it in userViewableTagIds }
        }
    }

    /**
     * Filters tags based on user's tag permissions
     * Returns only tags that the user can view (default tags + permitted household tags)
     */
    fun getUserViewableTags(
        context: UnitOfWorkContext,
        householdMemberId: String,
    ): List<Tag> {
        val permission = context.permissions.findByHouseholdMemberId(householdMemberId)

        // If no permission is set, user can't view any tags
        if (permission == null) {
            return emptyList()
        }

        // Get the tags that the user can view
        val userViewableTagIds = getUserViewableTagIds(context, permission.id)

        return context.tags.findByIds(userViewableTagIds.toList())
    }

    private fun getUserViewableTagIds(
        context: UnitOfWorkContext,
        permissionId: String,
    ): Set<String> {
        val userViewableTagPermissions = context.tagPermissions.findByPermissionId(permissionId)
        return userViewableTagPermissions.map { it.tagId }.toSet()
    }
}
