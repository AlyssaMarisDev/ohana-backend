package com.ohana.api.tags.models

import com.ohana.shared.exceptions.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GetTagsRequestTest {
    @Test
    fun `toDomain should return correct domain request with valid parameters`() {
        // Given
        val request =
            GetTagsRequest(
                householdId = "550e8400-e29b-41d4-a716-446655440000",
            )

        // When
        val domainRequest = request.toDomain()

        // Then
        assertEquals("550e8400-e29b-41d4-a716-446655440000", domainRequest.householdId)
    }

    @Test
    fun `toDomain should return correct domain request with null householdId`() {
        // Given
        val request = GetTagsRequest(householdId = null)

        // When
        val domainRequest = request.toDomain()

        // Then
        assertEquals(null, domainRequest.householdId)
    }

    @Test
    fun `toDomain should throw ValidationException with invalid householdId`() {
        // Given
        val request = GetTagsRequest(householdId = "invalid-uuid")

        // When & Then
        assertThrows<ValidationException> {
            request.toDomain()
        }
    }

    @Test
    fun `toDomain should work with empty string householdId`() {
        // Given
        val request = GetTagsRequest(householdId = "")

        // When & Then
        assertThrows<ValidationException> {
            request.toDomain()
        }
    }
}
