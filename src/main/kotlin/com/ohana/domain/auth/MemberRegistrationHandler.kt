package com.ohana.domain.auth

import com.ohana.data.auth.AuthMember
import com.ohana.data.auth.RefreshToken
import com.ohana.data.unitOfWork.*
import com.ohana.domain.auth.utils.*
import com.ohana.shared.exceptions.ConflictException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MemberRegistrationHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val name: String,
        val email: String,
        val password: String,
    )

    data class Response(
        val id: String,
        val accessToken: String,
        val refreshToken: String,
    )

    suspend fun handle(request: Request): Response {
        val salt = Hasher.generateSalt()
        val hashedPassword = Hasher.hashPassword(request.password, salt)
        val id = UUID.randomUUID().toString()

        return unitOfWork.execute { context ->
            // Validate that member doesn't already exist
            val existingMember = context.authMembers.findByEmail(request.email)
            if (existingMember != null) {
                throw ConflictException("Member with email ${request.email} already exists")
            }

            // Create the member
            val member =
                context.authMembers.create(
                    AuthMember(
                        id = id,
                        name = request.name,
                        email = request.email,
                        password = hashedPassword,
                        salt = salt,
                    ),
                )

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

            Response(member.id, tokenPair.accessToken, tokenPair.refreshToken)
        }
    }
}
