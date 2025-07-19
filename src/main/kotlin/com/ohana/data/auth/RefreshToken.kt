package com.ohana.data.auth

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant

data class RefreshToken(
    val id: String,
    val token: String,
    val userId: String,
    val expiresAt: Instant,
    val isRevoked: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val revokedAt: Instant? = null,
) {
    companion object {
        /**
         * Maps a database row to a RefreshToken object
         */
        val mapper: RowMapper<RefreshToken> =
            RowMapper { rs: ResultSet, _: StatementContext ->
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
}
