package com.ohana.data.household

import com.ohana.shared.enums.HouseholdMemberRole
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

data class HouseholdMember(
    val id: String,
    val householdId: String,
    val memberId: String,
    val role: HouseholdMemberRole,
    val isActive: Boolean = false,
    val invitedBy: String? = null,
    val joinedAt: java.time.Instant? = null,
) {
    companion object {
        /**
         * Maps a database row to a HouseholdMember object
         */
        val mapper: RowMapper<HouseholdMember> =
            RowMapper { rs: ResultSet, _: StatementContext ->
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
    }
}
