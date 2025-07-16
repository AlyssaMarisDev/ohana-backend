package com.ohana.data.task

interface TaskRepository {
    fun create(task: Task): Task

    fun findById(id: String): Task?

    fun findAll(): List<Task>

    fun findByHouseholdId(householdId: String): List<Task>

    fun findByHouseholdIds(householdIds: List<String>): List<Task>

    fun update(task: Task): Task
}
