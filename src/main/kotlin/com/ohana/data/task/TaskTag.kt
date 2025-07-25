package com.ohana.data.task

import java.time.Instant

data class TaskTag(
    val id: String,
    val taskId: String,
    val tagId: String,
    val createdAt: Instant,
)
