package com.ohana.task.entities

import com.ohana.shared.TaskStatus
import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Instant,
    val status: TaskStatus,
    val createdBy: String,
)
