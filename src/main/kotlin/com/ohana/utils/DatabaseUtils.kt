package com.ohana.utils

import com.ohana.exceptions.ConflictException
import com.ohana.exceptions.DbException
import com.ohana.exceptions.KnownError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.*
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.SQLIntegrityConstraintViolationException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
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

            val result =
                withContext(Dispatchers.IO) {
                    jdbi.inTransaction<T, Exception> { handle ->
                        try {
                            logger.debug("Starting block")
                            val blockResult = block(handle)
                            logger.debug("Block completed")

                            blockResult
                        } catch (e: Exception) {
                            logger.error("Transaction failed: ${e.message}", e)

                            if (e is KnownError) {
                                throw e
                            }

                            if (e.cause is SQLIntegrityConstraintViolationException) {
                                throw ConflictException("Duplicate entry", e)
                            }

                            throw DbException("Transaction failed: ${e.message}", e)
                        }
                    }
                }

            logger.debug("Transaction completed")

            return result
        }

        suspend fun <T> query(
            jdbi: Jdbi,
            block: (Handle) -> T,
        ): T {
            logger.debug("Starting handle")

            val result =
                withContext(Dispatchers.IO) {
                    jdbi.withHandle<T, Exception> { handle ->
                        try {
                            logger.debug("Starting block")
                            val blockResult = block(handle)
                            logger.debug("Block completed")

                            blockResult
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

            return result
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

            val inserted = insert.execute()

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

        fun <T : Any> get(
            handle: Handle,
            query: String,
            params: Map<String, Any>,
            clazz: KClass<T>,
        ): List<T> {
            logger.debug("Starting get")
            var get = handle.createQuery(query)

            params.forEach { (key, value) ->
                get = get.bind(key, value)
            }

            logger.debug("Getting")

            val results =
                get
                    .registerRowMapper(
                        clazz.java,
                        org.jdbi.v3.core.mapper.RowMapper { rs, _ ->
                            mapRowToObject(rs, clazz)
                        },
                    ).mapTo(clazz.java)
                    .toList()

            logger.debug("Get completed")

            return results
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

                    // Handle type conversions
                    when (parameter.type.classifier) {
                        java.time.Instant::class -> {
                            val timestamp = rs.getTimestamp(columnIndex)
                            timestamp?.toInstant()
                        }
                        else -> {
                            val classifier = parameter.type.classifier
                            if (classifier is KClass<*> && classifier.isSubclassOf(Enum::class)) {
                                val stringValue = rs.getString(columnIndex)
                                if (stringValue != null) {
                                    try {
                                        classifier.java
                                            .getMethod("valueOf", String::class.java)
                                            .invoke(null, stringValue) as Enum<*>
                                    } catch (e: Exception) {
                                        logger.warn("Failed to convert enum value: $stringValue for ${classifier.simpleName}")
                                        null
                                    }
                                } else {
                                    null
                                }
                            } else {
                                rs.getObject(columnIndex)
                            }
                        }
                    }
                }

            logger.debug("Mapping row to object: $args")

            // Create an instance using the constructor
            return constructor.callBy(args)
        }
    }
}
