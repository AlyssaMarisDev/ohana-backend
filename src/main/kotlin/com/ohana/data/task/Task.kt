package com.ohana.data.task

import com.ohana.shared.enums.TaskStatus
import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Instant?,
    val status: TaskStatus,
    val createdBy: String,
    val householdId: String,
)
