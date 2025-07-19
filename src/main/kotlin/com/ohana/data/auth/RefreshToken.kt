package com.ohana.data.auth

import java.time.Instant

data class RefreshToken(
    val id: String,
    val token: String,
    val userId: String,
    val expiresAt: Instant,
    val isRevoked: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val revokedAt: Instant? = null,
)
