package com.ohana.domain.auth

import com.ohana.data.auth.RefreshToken
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.*
import com.ohana.shared.exceptions.AuthorizationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class LoginHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val email: String,
        val password: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
    )

    suspend fun handle(request: Request): Response =
        unitOfWork.execute { context ->
            val member = context.authMembers.findByEmail(request.email)

            if (member == null || member.password != Hasher.hashPassword(request.password, member.salt)) {
                throw AuthorizationException("Invalid email or password")
            }

            // Generate token pair
            val tokenPair = JwtManager.generateTokenPair(member.id)

            // Store refresh token
            val refreshToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = tokenPair.refreshToken,
                    userId = member.id,
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
                )
            context.refreshTokens.create(refreshToken)

            Response(tokenPair.accessToken, tokenPair.refreshToken)
        }
}
