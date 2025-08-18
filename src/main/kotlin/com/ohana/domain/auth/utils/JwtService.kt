package com.ohana.domain.auth.utils

import com.auth0.jwt.interfaces.DecodedJWT

interface JwtService {
    fun validateRefreshToken(token: String): DecodedJWT?

    fun generateTokenPair(userId: String): TokenPair
}

class DefaultJwtService : JwtService {
    override fun validateRefreshToken(token: String): DecodedJWT? = JwtManager.validateRefreshToken(token)

    override fun generateTokenPair(userId: String): TokenPair = JwtManager.generateTokenPair(userId)
}
