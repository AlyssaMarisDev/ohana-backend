package com.ohana.domain.auth

import com.ohana.data.auth.RefreshToken
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.*
import com.ohana.shared.exceptions.AuthorizationException
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TokenRefreshHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Refresh token is required")
        val refreshToken: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
    )

    suspend fun handle(request: Request): Response =
        unitOfWork.execute { context ->
            // Validate the refresh token
            val decodedToken =
                JwtCreator.validateRefreshToken(request.refreshToken)
                    ?: throw AuthorizationException("Invalid refresh token")

            val userId =
                decodedToken.getClaim("userId").asString()
                    ?: throw AuthorizationException("Invalid refresh token")

            // Check if refresh token exists in database and is not revoked
            val storedToken =
                context.refreshTokens.findByToken(request.refreshToken)
                    ?: throw AuthorizationException("Refresh token not found")

            if (storedToken.isRevoked) {
                throw AuthorizationException("Refresh token has been revoked")
            }

            if (storedToken.expiresAt.isBefore(Instant.now())) {
                throw AuthorizationException("Refresh token has expired")
            }

            // Verify the token belongs to the user
            if (storedToken.userId != userId) {
                throw AuthorizationException("Invalid refresh token")
            }

            // Revoke the old refresh token (token rotation)
            context.refreshTokens.revokeToken(request.refreshToken)

            // Generate new token pair
            val newTokenPair = JwtCreator.generateTokenPair(userId)

            // Store new refresh token
            val newRefreshToken =
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    token = newTokenPair.refreshToken,
                    userId = userId,
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
                )
            context.refreshTokens.create(newRefreshToken)

            Response(newTokenPair.accessToken, newTokenPair.refreshToken)
        }
}
