package com.ohana.data.utils

import org.jdbi.v3.core.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class DatabaseUtils(
    private val jdbi: Jdbi,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

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

        fun delete(
            handle: Handle,
            deleteQuery: String,
            params: Map<String, Any?>,
        ): Int {
            logger.debug("Starting delete")
            var delete = handle.createUpdate(deleteQuery)

            params.forEach { (key, value) ->
                delete = delete.bind(key, value)
            }

            logger.debug("Deleting")

            val deleted = delete.execute()

            logger.debug("Delete completed")

            return deleted
        }

        /**
         * Generic query method using JDBI's built-in row mapping for data classes
         * This is simpler and more performant than custom reflection-based mapping
         */
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

            // Use JDBI's built-in row mapping for data classes
            val results = get.mapTo(clazz.java).toList()

            logger.debug("Get completed")

            return results
        }

        /**
         * Query method using custom row mappers for better control and performance
         * Use this when you need custom mapping logic or better error handling
         */
        fun <T : Any> getWithMapper(
            handle: Handle,
            query: String,
            params: Map<String, Any>,
            rowMapper: RowMapper<T>,
        ): List<T> {
            logger.debug("Starting get with custom mapper")
            var get = handle.createQuery(query)

            params.forEach { (key, value) ->
                get = get.bind(key, value)
            }

            logger.debug("Getting")

            val results = get.map(rowMapper).toList()

            logger.debug("Get completed")

            return results
        }
    }
}
