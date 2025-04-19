package com.ohana.utils

import com.ohana.exceptions.DbException
import com.ohana.exceptions.KnownError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

class DatabaseUtils(
    private val jdbi: Jdbi,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        suspend fun <T> transaction(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T =
            withContext(Dispatchers.IO) {
                jdbi.inTransaction<T, Exception> { handle ->
                    try {
                        block(handle)
                    } catch (e: Exception) {
                        if (e is KnownError) {
                            throw e
                        }

                        throw DbException("Transaction failed: ${e.message}", e)
                    }
                }
            }

        suspend fun <T> query(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T =
            withContext(Dispatchers.IO) {
                jdbi.withHandle<T, Exception> { handle ->
                    try {
                        block(handle)
                    } catch (e: Exception) {
                        if (e is KnownError) {
                            throw e
                        }

                        throw DbException("Query failed: ${e.message}", e)
                    }
                }
            }
    }
}
