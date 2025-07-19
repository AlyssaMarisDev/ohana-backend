package com.ohana.data.auth

import com.ohana.data.utils.DatabaseUtils
import com.ohana.data.utils.RowMappers
import com.ohana.shared.exceptions.DbException
import com.ohana.shared.exceptions.NotFoundException
import org.jdbi.v3.core.Handle
import java.time.Instant

class JdbiRefreshTokenRepository(
    private val handle: Handle,
) : RefreshTokenRepository {
    override fun create(refreshToken: RefreshToken): RefreshToken {
        val insertQuery = """
            INSERT INTO refresh_tokens (id, token, user_id, expires_at, is_revoked, created_at)
            VALUES (:id, :token, :user_id, :expires_at, :is_revoked, :created_at)
        """

        val insertedRows =
            DatabaseUtils.insert(
                handle,
                insertQuery,
                mapOf(
                    "id" to refreshToken.id,
                    "token" to refreshToken.token,
                    "user_id" to refreshToken.userId,
                    "expires_at" to refreshToken.expiresAt,
                    "is_revoked" to refreshToken.isRevoked,
                    "created_at" to refreshToken.createdAt,
                ),
            )

        if (insertedRows == 0) throw DbException("Failed to create refresh token")

        return findById(refreshToken.id) ?: throw NotFoundException("Refresh token not found after creation")
    }

    override fun findByToken(token: String): RefreshToken? {
        val selectQuery = """
            SELECT id, token, user_id, expires_at, is_revoked, created_at, revoked_at
            FROM refresh_tokens
            WHERE token = :token
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("token" to token),
                RowMappers.refreshTokenMapper,
            ).firstOrNull()
    }

    override fun findByUserId(userId: String): List<RefreshToken> {
        val selectQuery = """
            SELECT id, token, user_id, expires_at, is_revoked, created_at, revoked_at
            FROM refresh_tokens
            WHERE user_id = :user_id
            ORDER BY created_at DESC
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("user_id" to userId),
                RowMappers.refreshTokenMapper,
            )
    }

    override fun revokeToken(token: String): Boolean {
        val updateQuery = """
            UPDATE refresh_tokens
            SET is_revoked = true, revoked_at = :revoked_at
            WHERE token = :token
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "token" to token,
                    "revoked_at" to Instant.now(),
                ),
            )

        return updatedRows > 0
    }

    override fun revokeAllUserTokens(userId: String): Boolean {
        val updateQuery = """
            UPDATE refresh_tokens
            SET is_revoked = true, revoked_at = :revoked_at
            WHERE user_id = :user_id AND is_revoked = false
        """

        val updatedRows =
            DatabaseUtils.update(
                handle,
                updateQuery,
                mapOf(
                    "user_id" to userId,
                    "revoked_at" to Instant.now(),
                ),
            )

        return updatedRows > 0
    }

    override fun deleteExpiredTokens(): Int {
        val deleteQuery = """
            DELETE FROM refresh_tokens
            WHERE expires_at < :now
        """

        return DatabaseUtils.delete(
            handle,
            deleteQuery,
            mapOf("now" to Instant.now()),
        )
    }

    private fun findById(id: String): RefreshToken? {
        val selectQuery = """
            SELECT id, token, user_id, expires_at, is_revoked, created_at, revoked_at
            FROM refresh_tokens
            WHERE id = :id
        """

        return DatabaseUtils
            .getWithMapper(
                handle,
                selectQuery,
                mapOf("id" to id),
                RowMappers.refreshTokenMapper,
            ).firstOrNull()
    }
}
