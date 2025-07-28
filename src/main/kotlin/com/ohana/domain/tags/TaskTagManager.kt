package com.ohana.domain.tags

import com.ohana.data.tags.Tag
import com.ohana.data.tags.TaskTag
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.shared.exceptions.ValidationException
import java.time.Instant
import java.util.UUID

class TaskTagManager {
    /**
     * Assigns tags to a task, replacing any existing tags
     * Returns the list of created task tags
     */
    fun assignTagsToTask(
        context: UnitOfWorkContext,
        taskId: String,
        tagIds: List<String>,
    ): List<Tag> {
        context.taskTags.deleteByTaskId(taskId)

        if (tagIds.isEmpty()) {
            return emptyList()
        }

        val task =
            context.tasks.findById(taskId)
                ?: throw ValidationException("Task with ID $taskId not found")

        val tags = context.tags.findByIds(tagIds)

        // Check if all requested tags were found
        if (tags.size != tagIds.size) {
            val foundTagIds = tags.map { it.id }.toSet()
            val missingTagIds = tagIds.filter { !foundTagIds.contains(it) }
            throw ValidationException("Tag ${missingTagIds.first()} not found")
        }

        tags.forEach { tag ->
            // All tags must belong to the same household as the task
            if (tag.householdId != task.householdId) {
                throw ValidationException("Tag ${tag.id} does not belong to the same household as task $taskId")
            }
        }

        val taskTags =
            tagIds.map { tagId ->
                TaskTag(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    tagId = tagId,
                    createdAt = Instant.now(),
                )
            }

        context.taskTags.createMany(taskTags)

        return tags
    }

    /**
     * Gets task tags for a task
     */
    fun getTaskTags(
        context: UnitOfWorkContext,
        taskId: String,
    ): List<Tag> {
        val taskTags = context.taskTags.findByTaskId(taskId)
        return context.tags.findByIds(taskTags.map { it.tagId })
    }

    /**
     * Gets task tags for multiple tasks
     */
    fun getTasksTags(
        context: UnitOfWorkContext,
        taskIds: List<String>,
    ): Map<String, List<Tag>> {
        if (taskIds.isEmpty()) {
            return emptyMap()
        }

        val taskTags = context.taskTags.findByTaskIds(taskIds)
        val tagIds = taskTags.map { it.tagId }
        val tags = if (tagIds.isEmpty()) emptyList() else context.tags.findByIds(tagIds)
        val tagMap = tags.associateBy { it.id }

        return taskIds.associateWith { taskId ->
            taskTags
                .filter { it.taskId == taskId }
                .mapNotNull { taskTag -> tagMap[taskTag.tagId] }
        }
    }
}
