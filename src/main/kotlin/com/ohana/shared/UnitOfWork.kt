package com.ohana.shared

import com.ohana.member.entities.Member
import com.ohana.task.entities.Task

// Unit of Work interface
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

// Unit of Work context interface
interface UnitOfWorkContext {
    val tasks: TaskRepository
    val members: MemberRepository
}

// Repository interfaces
interface TaskRepository {
    fun create(task: Task): Task

    fun findById(id: String): Task?
}

interface MemberRepository {
    fun findById(id: String): Member?
}
