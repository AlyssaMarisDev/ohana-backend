package com.ohana.utils

import com.ohana.exceptions.DbException
import com.ohana.exceptions.KnownError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.*
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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

        fun insert(
            handle: Handle,
            insertQuery: String,
            params: Map<String, Any?>,
        ): Int {
            var insert = handle.createUpdate(insertQuery)

            params.forEach { (key, value) ->
                insert = insert.bind(key, value)
            }

            return insert
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Int::class.java)
                .findOne()
                .orElseThrow { throw DbException("Failed to insert") }
        }

        fun update(
            handle: Handle,
            updateQuery: String,
            params: Map<String, Any?>,
        ): Int {
            var update = handle.createUpdate(updateQuery)

            params.forEach { (key, value) ->
                update = update.bind(key, value)
            }

            return update.execute()
        }

        fun <T : Any> fetch(
            handle: Handle,
            query: String,
            params: Map<String, Any>,
            clazz: KClass<T>,
        ): List<T> {
            var fetch = handle.createQuery(query)

            params.forEach { (key, value) ->
                fetch = fetch.bind(key, value)
            }

            return fetch
                .map { rs, _ ->
                    mapRowToObject(rs, clazz)
                }.toList()
        }

        private fun <T : Any> mapRowToObject(
            rs: ResultSet,
            clazz: KClass<T>,
        ): T {
            // Get the primary constructor
            val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException("Class must have a primary constructor")
            constructor.isAccessible = true

            // Get ResultSetMetaData
            val metaData = rs.metaData
            val columnCount = metaData.columnCount

            // Cache column indices in a map
            val columnIndexMap = (1..columnCount).associateBy { metaData.getColumnName(it).lowercase() }

            // Create a map of parameter names to values
            val args =
                constructor.parameters.associateWith { parameter ->
                    val columnName = parameter.name?.lowercase() ?: throw IllegalArgumentException("Parameter must have a name")
                    val columnIndex =
                        columnIndexMap[columnName]
                            ?: throw IllegalArgumentException("Column $columnName not found in result set")
                    rs.getObject(columnIndex)
                }

            // Create an instance using the constructor
            return constructor.callBy(args)
        }
    }
}
