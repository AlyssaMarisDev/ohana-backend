package com.ohana.shared

import com.ohana.member.repositories.JdbiMemberRepository
import com.ohana.task.repositories.JdbiTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

// JDBI Unit of Work implementation
class JdbiUnitOfWork(
    private val jdbi: Jdbi,
) : UnitOfWork {
    override suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T =
        withContext(Dispatchers.IO) {
            jdbi.inTransaction<T, Exception> { handle ->
                val context = JdbiUnitOfWorkContext(handle)
                block(context)
            }
        }
}

// JDBI Unit of Work context implementation
class JdbiUnitOfWorkContext(
    private val handle: Handle,
) : UnitOfWorkContext {
    override val tasks: TaskRepository = JdbiTaskRepository(handle)
    override val members: MemberRepository = JdbiMemberRepository(handle)
}
