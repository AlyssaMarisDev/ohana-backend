package com.ohana.data.tags

interface TaskTagRepository {
    fun findByTaskId(taskId: String): List<TaskTag>

    fun findByTaskIds(taskIds: List<String>): List<TaskTag>

    fun create(taskTag: TaskTag): TaskTag

    fun createMany(taskTags: List<TaskTag>): List<TaskTag>

    fun deleteByTaskId(taskId: String): Boolean

    fun deleteByTaskIdAndTagIds(
        taskId: String,
        tagIds: List<String>,
    ): Boolean

    fun deleteByTagId(tagId: String): Boolean
}
