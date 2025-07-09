package com.ohana.utils

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.sql.SQLException

object EnumMapper {
    /**
     * Creates a column mapper for an enum that maps database strings to enum values
     */
    inline fun <reified T : Enum<T>> createEnumMapper(): ColumnMapper<T> =
        ColumnMapper { rs: ResultSet, columnNumber: Int, ctx: StatementContext ->
            val value = rs.getString(columnNumber)
            if (value == null) {
                null
            } else {
                try {
                    enumValueOf<T>(value.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw SQLException("Invalid enum value: $value for enum ${T::class.simpleName}")
                }
            }
        }

    /**
     * Creates a column mapper for an enum that maps database strings to enum values (case-insensitive)
     */
    inline fun <reified T : Enum<T>> createCaseInsensitiveEnumMapper(): ColumnMapper<T> =
        ColumnMapper { rs: ResultSet, columnNumber: Int, ctx: StatementContext ->
            val value = rs.getString(columnNumber)
            if (value == null) {
                null
            } else {
                try {
                    enumValueOf<T>(value.uppercase())
                } catch (e: IllegalArgumentException) {
                    // Try to find by name ignoring case
                    T::class.java.enumConstants.find {
                        it.name.equals(value, ignoreCase = true)
                    } ?: throw SQLException("Invalid enum value: $value for enum ${T::class.simpleName}")
                }
            }
        }
}
