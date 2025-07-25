package com.ohana.api.auth.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TokenRefreshRequestTest {
    @Test
    fun `toDomain should pass when refresh token is valid`() =
        runTest {
            val request =
                TokenRefreshRequest(
                    refreshToken = "valid.refresh.token.here",
                )

            val domainRequest = request.toDomain()

            assertEquals("valid.refresh.token.here", domainRequest.refreshToken)
        }

    @Test
    fun `toDomain should throw ValidationException when refresh token is null`() =
        runTest {
            val request =
                TokenRefreshRequest(
                    refreshToken = null,
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("refreshToken", exception.errors!![0].field)
            assertEquals("Refresh token is required", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when refresh token is blank`() =
        runTest {
            val request =
                TokenRefreshRequest(
                    refreshToken = "",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("refreshToken", exception.errors!![0].field)
            assertEquals("Refresh token cannot be blank", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should accept any non-blank refresh token`() =
        runTest {
            val validTokens =
                listOf(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                    "simple.token",
                    "a",
                    "very.long.token.with.many.parts.and.dots",
                )

            validTokens.forEach { token ->
                val request =
                    TokenRefreshRequest(
                        refreshToken = token,
                    )

                val domainRequest = request.toDomain()
                assertEquals(token, domainRequest.refreshToken)
            }
        }
}
