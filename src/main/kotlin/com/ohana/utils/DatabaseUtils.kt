package com.ohana.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

class DatabaseUtils(
    private val jdbi: Jdbi,
) {
    companion object {
        suspend fun <T> transaction(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T =
            withContext(Dispatchers.IO) {
                jdbi.inTransaction<T, Exception> { handle ->
                    block(handle)
                }
            }

        suspend fun <T> query(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T =
            withContext(Dispatchers.IO) {
                jdbi.withHandle<T, Exception> { handle ->
                    block(handle)
                }
            }
    }
}
