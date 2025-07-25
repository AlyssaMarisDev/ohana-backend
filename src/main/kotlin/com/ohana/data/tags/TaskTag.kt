package com.ohana.data.tags

import java.time.Instant

data class TaskTag(
    val id: String,
    val taskId: String,
    val tagId: String,
    val createdAt: Instant,
)
