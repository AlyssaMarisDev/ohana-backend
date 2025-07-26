package com.ohana.data.task

import java.time.Instant

interface TaskRepository {
    fun create(task: Task): Task

    fun findById(id: String): Task?

    fun findAll(): List<Task>

    fun findByHouseholdId(householdId: String): List<Task>

    fun findByHouseholdIds(householdIds: List<String>): List<Task>

    fun findByHouseholdIdsWithDateFilters(
        householdIds: List<String>,
        dueDateFrom: Instant?,
        dueDateTo: Instant?,
        completedDateFrom: Instant?,
        completedDateTo: Instant?,
    ): List<Task>

    fun update(task: Task): Task

    fun deleteById(id: String): Boolean
}
