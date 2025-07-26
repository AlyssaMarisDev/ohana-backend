package com.ohana.domain.tags

import com.ohana.data.tags.TagPermission
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.shared.enums.TagPermissionType
import java.time.Instant
import java.util.UUID

class TagPermissionManager {
    /**
     * Creates or updates tag permissions for a household member
     */
    fun setTagPermissions(
        context: UnitOfWorkContext,
        householdMemberId: String,
        permissionType: TagPermissionType,
        tagIds: List<String>,
    ): TagPermission {
        val existingPermission = context.tagPermissions.findByHouseholdMemberId(householdMemberId)

        val permission =
            TagPermission(
                id = existingPermission?.id ?: UUID.randomUUID().toString(),
                householdMemberId = householdMemberId,
                permissionType = permissionType,
                tagIds = tagIds,
                createdAt = existingPermission?.createdAt ?: Instant.now(),
                updatedAt = Instant.now(),
            )

        return if (existingPermission != null) {
            context.tagPermissions.update(permission)
        } else {
            context.tagPermissions.create(permission)
        }
    }

    /**
     * Gets tag permissions for a household member
     */
    fun getTagPermissions(
        context: UnitOfWorkContext,
        householdMemberId: String,
    ): TagPermission? = context.tagPermissions.findByHouseholdMemberId(householdMemberId)

    /**
     * Deletes tag permissions for a household member
     */
    fun deleteTagPermissions(
        context: UnitOfWorkContext,
        householdMemberId: String,
    ): Boolean = context.tagPermissions.deleteByHouseholdMemberId(householdMemberId)

    /**
     * Filters tags based on user permissions
     * Returns the list of tag IDs that the user is allowed to view
     */
    fun filterTagsByPermissions(
        context: UnitOfWorkContext,
        householdMemberId: String,
        allTagIds: List<String>,
    ): List<String> {
        val permission = context.tagPermissions.findByHouseholdMemberId(householdMemberId)

        // If no permission is set, user can view all tags (default behavior)
        if (permission == null) {
            return allTagIds
        }

        return when (permission.permissionType) {
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
        val taskTags = context.taskTags.findByTaskIds(taskIds)
        val taskTagMap = taskTags.groupBy { it.taskId }

        return taskIds.filter { taskId ->
            val taskTagIds = taskTagMap[taskId]?.map { it.tagId } ?: emptyList()

            // If task has no tags, user can view it (default behavior)
            if (taskTagIds.isEmpty()) {
                return@filter true
            }

            // Check if user can view at least one tag of the task
            taskTagIds.any { tagId -> canViewTag(permission, tagId) }
        }
    }

    /**
     * Checks if a user can view a specific tag
     */
    private fun canViewTag(
        permission: TagPermission,
        tagId: String,
    ): Boolean =
        when (permission.permissionType) {
            TagPermissionType.ALLOW_ALL_EXCEPT -> {
                // User can view all tags except those in the exception list
                !permission.tagIds.contains(tagId)
            }
            TagPermissionType.DENY_ALL_EXCEPT -> {
                // User can only view tags in the exception list
                permission.tagIds.contains(tagId)
            }
        }
}
