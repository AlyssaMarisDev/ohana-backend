package com.ohana.domain.permissions

import com.ohana.data.permissions.Permission
import com.ohana.data.permissions.TagPermission
import com.ohana.data.tags.Tag
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.domain.tags.TaskTagManager
import java.time.Instant
import java.util.UUID

class TagPermissionManager(
    private val taskTagManager: TaskTagManager,
) {
    /**
     * Creates permissions for a household member and gives them access to specified tags
     */
    fun createPermissionsWithTags(
        context: UnitOfWorkContext,
        householdMemberId: String,
        tagIds: List<String>,
    ): Permission {
        // Create permission for the household member
        val permission =
            context.permissions.create(
                Permission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )

        // Create tag permissions for the specified tags
        tagIds.forEach { tagId ->
            context.tagPermissions.create(
                TagPermission(
                    id = UUID.randomUUID().toString(),
                    permissionId = permission.id,
                    tagId = tagId,
                    createdAt = Instant.now(),
                ),
            )
        }

        return permission
    }

    /**
     * Gives an existing household member permission to view specific tags
     * Assumes the member already has a permission record
     */
    fun giveTagPermissionsToMember(
        context: UnitOfWorkContext,
        householdMemberId: String,
        tagIds: List<String>,
    ) {
        // Get the existing permission for the household member
        val permission =
            context.permissions.findByHouseholdMemberId(householdMemberId)
                ?: throw IllegalArgumentException("No permission record found for household member")

        // Get existing tag permissions to avoid duplicates
        val existingTagPermissions = context.tagPermissions.findByPermissionId(permission.id)
        val existingTagIds = existingTagPermissions.map { it.tagId }.toSet()

        // Create tag permissions for tags that don't already exist
        tagIds.forEach { tagId ->
            if (tagId !in existingTagIds) {
                context.tagPermissions.create(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = tagId,
                        createdAt = Instant.now(),
                    ),
                )
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

        val permission = context.permissions.findByHouseholdMemberId(householdMemberId)

        // If no permission is set, user can't view any tasks
        if (permission == null) {
            return emptyList()
        }

        // Get the tags that the user can view
        val userViewableTagPermissions = context.tagPermissions.findByPermissionId(permission.id)
        val userViewableTagIds = userViewableTagPermissions.map { it.tagId }.toSet()

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
     * Returns only tags that the user can view (household tags they have permission for)
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
        val userViewableTagPermissions = context.tagPermissions.findByPermissionId(permission.id)
        val userViewableTagIds = userViewableTagPermissions.map { it.tagId }.toSet()

        return context.tags.findByIds(userViewableTagIds.toList())
    }
}
