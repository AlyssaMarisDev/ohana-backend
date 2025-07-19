package com.ohana.data.task

import com.ohana.shared.enums.TaskStatus
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Instant,
    val status: TaskStatus,
    val createdBy: String,
    val householdId: String,
) {
    companion object {
        /**
         * Maps a database row to a Task object
         */
        val mapper: RowMapper<Task> =
            RowMapper { rs: ResultSet, _: StatementContext ->
                Task(
                    id = rs.getString("id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    dueDate = rs.getTimestamp("due_date")?.toInstant() ?: Instant.now(),
                    status = TaskStatus.valueOf(rs.getString("status").uppercase()),
                    createdBy = rs.getString("created_by"),
                    householdId = rs.getString("household_id"),
                )
            }
    }
}
