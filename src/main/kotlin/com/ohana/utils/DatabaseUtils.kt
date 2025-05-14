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
        ): T {
            logger.debug("Starting transaction")
            var result: T? = null

            withContext(Dispatchers.IO) {
                jdbi.inTransaction<T, Exception> { handle ->

                    try {
                        logger.debug("Starting block")
                        result = block(handle)
                        logger.debug("Block completed")

                        result
                    } catch (e: Exception) {
                        logger.error("Transaction failed: ${e.message}", e)

                        if (e is KnownError) {
                            throw e
                        }

                        throw DbException("Transaction failed: ${e.message}", e)
                    }
                }
            }

            logger.debug("Transaction completed")

            return result ?: throw DbException("Transaction failed: No result returned")
        }

        suspend fun <T> query(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T {
            logger.debug("Starting handle")
            var result: T? = null

            withContext(Dispatchers.IO) {
                jdbi.withHandle<T, Exception> { handle ->
                    try {
                        logger.debug("Starting block")
                        result = block(handle)
                        logger.debug("Block completed")

                        result
                    } catch (e: Exception) {
                        logger.error("Query failed: ${e.message}", e)

                        if (e is KnownError) {
                            throw e
                        }

                        throw DbException("Query failed: ${e.message}", e)
                    }
                }
            }

            logger.debug("Handle completed")

            return result ?: throw DbException("Handle failed: No result returned")
        }

        fun insert(
            handle: Handle,
            insertQuery: String,
            params: Map<String, Any?>,
        ): Int {
            logger.debug("Starting insert")
            var insert = handle.createUpdate(insertQuery)

            params.forEach { (key, value) ->
                insert = insert.bind(key, value)
            }

            logger.debug("Inserting")

            val inserted =
                insert
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java)
                    .findOne()
                    .orElseThrow { throw DbException("Failed to insert") }

            logger.debug("Insert completed")

            return inserted
        }

        fun update(
            handle: Handle,
            updateQuery: String,
            params: Map<String, Any?>,
        ): Int {
            logger.debug("Starting update")
            var update = handle.createUpdate(updateQuery)

            params.forEach { (key, value) ->
                update = update.bind(key, value)
            }

            logger.debug("Updating")

            val updated = update.execute()

            logger.debug("Update completed")

            return updated
        }

        fun <T : Any> fetch(
            handle: Handle,
            query: String,
            params: Map<String, Any>,
            clazz: KClass<T>,
        ): List<T> {
            logger.debug("Starting fetch")
            var fetch = handle.createQuery(query)

            params.forEach { (key, value) ->
                fetch = fetch.bind(key, value)
            }

            logger.debug("Fetching")

            val fetched =
                fetch
                    .map { rs, _ ->
                        mapRowToObject(rs, clazz)
                    }.toList()

            logger.debug("Fetch completed")

            return fetched
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
