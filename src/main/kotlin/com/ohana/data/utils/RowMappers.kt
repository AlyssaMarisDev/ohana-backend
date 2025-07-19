package com.ohana.data.utils

import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.RefreshToken
import com.ohana.data.household.Household
import com.ohana.data.household.HouseholdMember
import com.ohana.data.member.Member
import com.ohana.data.task.Task
import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.TaskStatus
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

/**
 * Row mappers for converting database rows to domain objects.
 * These provide better performance and type safety than reflection-based mapping.
 */
object RowMappers {
    /**
     * Maps a database row to a Task object
     */
    val taskMapper =
        RowMapper<Task> { rs: ResultSet, _: StatementContext ->
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

    /**
     * Maps a database row to a Member object
     */
    val memberMapper =
        RowMapper<Member> { rs: ResultSet, _: StatementContext ->
            Member(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                age = rs.getObject("age") as? Int,
                gender = rs.getString("gender"),
            )
        }

    /**
     * Maps a database row to an AuthMember object
     */
    val authMemberMapper =
        RowMapper<AuthMember> { rs: ResultSet, _: StatementContext ->
            AuthMember(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                password = rs.getString("password"),
                salt = rs.getBytes("salt"),
                age = rs.getObject("age") as? Int,
                gender = rs.getString("gender"),
            )
        }

    /**
     * Maps a database row to a Household object
     */
    val householdMapper =
        RowMapper<Household> { rs: ResultSet, _: StatementContext ->
            Household(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                createdBy = rs.getString("created_by"),
            )
        }

    /**
     * Maps a database row to a HouseholdMember object
     */
    val householdMemberMapper =
        RowMapper<HouseholdMember> { rs: ResultSet, _: StatementContext ->
            HouseholdMember(
                id = rs.getString("id"),
                householdId = rs.getString("household_id"),
                memberId = rs.getString("member_id"),
                role = HouseholdMemberRole.valueOf(rs.getString("role").uppercase()),
                isActive = rs.getBoolean("is_active"),
                invitedBy = rs.getString("invited_by"),
                joinedAt = rs.getTimestamp("joined_at")?.toInstant(),
            )
        }

    /**
     * Maps a database row to a RefreshToken object
     */
    val refreshTokenMapper =
        RowMapper<RefreshToken> { rs: ResultSet, _: StatementContext ->
            RefreshToken(
                id = rs.getString("id"),
                token = rs.getString("token"),
                userId = rs.getString("user_id"),
                expiresAt = rs.getTimestamp("expires_at")?.toInstant() ?: Instant.now(),
                isRevoked = rs.getBoolean("is_revoked"),
                createdAt = rs.getTimestamp("created_at")?.toInstant() ?: Instant.now(),
                revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
            )
        }
}
