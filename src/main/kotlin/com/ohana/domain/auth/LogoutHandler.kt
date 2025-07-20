package com.ohana.domain.auth

import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.*
import com.ohana.shared.exceptions.AuthorizationException

class LogoutHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val refreshToken: String,
    )

    data class Response(
        val message: String,
    )

    suspend fun handle(request: Request): Response =
        unitOfWork.execute { context ->
            // Validate the refresh token
            val decodedToken =
                JwtManager.validateRefreshToken(request.refreshToken)
                    ?: throw AuthorizationException("Invalid refresh token")

            val userId =
                decodedToken.getClaim("userId").asString()
                    ?: throw AuthorizationException("Invalid refresh token")

            // Check if refresh token exists in database
            val storedToken =
                context.refreshTokens.findByToken(request.refreshToken)
                    ?: throw AuthorizationException("Refresh token not found")

            // Verify the token belongs to the user
            if (storedToken.userId != userId) {
                throw AuthorizationException("Invalid refresh token")
            }

            // Revoke the refresh token
            context.refreshTokens.revokeToken(request.refreshToken)

            Response("Successfully logged out")
        }
}
