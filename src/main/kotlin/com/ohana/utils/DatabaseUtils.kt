package com.ohana.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.Handle

class TransactionHandler(private val jdbi: Jdbi) {

    companion object {
        suspend fun <T> transaction(jdbi: Jdbi, block: (Handle) -> T): T {
            return withContext(Dispatchers.IO) {
                jdbi.inTransaction<T, Exception> { handle ->
                    block(handle)
                }
            }
        }

        suspend fun <T> query(jdbi: Jdbi, block: (Handle) -> T): T {
            return withContext(Dispatchers.IO) {
                jdbi.withHandle<T, Exception> { handle ->
                    block(handle)
                }
            }
        }
    }
}