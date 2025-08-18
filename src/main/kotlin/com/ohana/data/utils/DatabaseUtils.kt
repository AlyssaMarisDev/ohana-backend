package com.ohana.data.utils

import com.ohana.shared.exceptions.ConflictException
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.KnownError
import org.jdbi.v3.core.Handle
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

private val logger = LoggerFactory.getLogger("DatabaseUtils")

inline fun <T> translateDbErrors(block: () -> T): T {
    try {
        return block()
    } catch (throwable: Throwable) {
        // Pass through already-translated, known domain errors
        if (throwable is KnownError) {
            throw throwable
        }

        val root = findSqlCause(throwable)

        // Handle integrity and duplicate key violations as conflicts
        if (isIntegrityConstraintViolation(root)) {
            throw ConflictException(
                message = root?.message ?: throwable.message ?: "Database constraint violated",
                cause = throwable,
            )
        }

        // Fallback: unknown SQL or DB-layer error
        throw DbException(
            message = throwable.message ?: "Database operation failed",
            cause = throwable,
        )
    }
}

@PublishedApi
internal fun findSqlCause(throwable: Throwable?): java.sql.SQLException? {
    var current: Throwable? = throwable
    while (current != null) {
        if (current is java.sql.SQLException) return current
        current = current.cause
    }
    return null
}

@PublishedApi
internal fun isIntegrityConstraintViolation(sqlException: java.sql.SQLException?): Boolean {
    if (sqlException == null) return false

    // SQLState class 23 — Integrity Constraint Violation (ANSI)
    if (sqlException.sqlState?.startsWith("23") == true) return true

    // Common MySQL vendor codes
    // 1062: Duplicate entry for key (unique constraint)
    // 1451/1452: Cannot delete or add a child row (foreign key constraint)
    val vendorCode = sqlException.errorCode
    if (vendorCode == 1062 || vendorCode == 1451 || vendorCode == 1452) return true

    // Also directly check for SQLIntegrityConstraintViolationException subclasses
    return sqlException is java.sql.SQLIntegrityConstraintViolationException
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

    val results = get.mapTo(clazz.java).toList()

    logger.debug("Get completed")

    return results
}
