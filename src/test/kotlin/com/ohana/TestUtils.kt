package com.ohana

import com.ohana.member.entities.Member
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import com.ohana.task.entities.Task
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant

class TestUtils {
    companion object {
        suspend fun mockUnitOfWork(
            unitOfWork: UnitOfWork,
            context: UnitOfWorkContext,
        ) {
            whenever(unitOfWork.execute<Any>(any())).thenAnswer { invocation ->
                val block = invocation.getArgument<(UnitOfWorkContext) -> Any>(0)
                block.invoke(context)
            }
        }

        fun getTask(
            id: String = "task-1",
            title: String = "Test Task",
            description: String = "Test Description",
            dueDate: Instant = Instant.now(),
            status: TaskStatus = TaskStatus.pending,
            createdBy: String = "user-1",
        ): Task = Task(id, title, description, dueDate, status, createdBy)

        fun getMember(
            id: String = "user-1",
            name: String = "Test User",
            email: String = "test@example.com",
            age: Int? = null,
            gender: String? = null,
        ): Member = Member(id, name, email, age, gender)
    }
}
