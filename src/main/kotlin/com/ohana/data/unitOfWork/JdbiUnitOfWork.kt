package com.ohana.data.unitOfWork

import com.ohana.data.auth.AuthMemberRepository
import com.ohana.data.auth.JdbiAuthMemberRepository
import com.ohana.data.auth.JdbiRefreshTokenRepository
import com.ohana.data.auth.RefreshTokenRepository
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.household.JdbiHouseholdRepository
import com.ohana.data.member.JdbiMemberRepository
import com.ohana.data.member.MemberRepository
import com.ohana.data.tags.JdbiTagRepository
import com.ohana.data.tags.JdbiTaskTagRepository
import com.ohana.data.tags.TagRepository
import com.ohana.data.tags.TaskTagRepository
import com.ohana.data.task.JdbiTaskRepository
import com.ohana.data.task.TaskRepository
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
    override val taskTags: TaskTagRepository = JdbiTaskTagRepository(handle)
    override val members: MemberRepository = JdbiMemberRepository(handle)
    override val households: HouseholdRepository = JdbiHouseholdRepository(handle)
    override val tags: TagRepository = JdbiTagRepository(handle)
    override val authMembers: AuthMemberRepository = JdbiAuthMemberRepository(handle)
    override val refreshTokens: RefreshTokenRepository = JdbiRefreshTokenRepository(handle)
}
