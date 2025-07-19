package com.ohana.data.auth

interface RefreshTokenRepository {
    fun create(refreshToken: RefreshToken): RefreshToken

    fun findByToken(token: String): RefreshToken?

    fun findByUserId(userId: String): List<RefreshToken>

    fun revokeToken(token: String): Boolean

    fun revokeAllUserTokens(userId: String): Boolean

    fun deleteExpiredTokens(): Int
}
