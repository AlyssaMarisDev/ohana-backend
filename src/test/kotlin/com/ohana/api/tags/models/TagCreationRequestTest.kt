package com.ohana.api.tags.models

import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TagCreationRequestTest {
    @Test
    fun `toDomain should pass when all fields are valid`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "#FF0000",
                )

            val domainRequest = request.toDomain()

            assertEquals("Important", domainRequest.name)
            assertEquals("#FF0000", domainRequest.color)
        }

    @Test
    fun `toDomain should trim name and uppercase color`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "  Important  ",
                    color = "#ff0000",
                )

            val domainRequest = request.toDomain()

            assertEquals("Important", domainRequest.name)
            assertEquals("#FF0000", domainRequest.color)
        }

    @Test
    fun `toDomain should throw ValidationException when name is blank`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "",
                    color = "#FF0000",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Tag name is required and must be between 1 and 50 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is whitespace only`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "   ",
                    color = "#FF0000",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Tag name is required and must be between 1 and 50 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when name is too long`() =
        runTest {
            val longName = "a".repeat(51)
            val request =
                TagCreationRequest(
                    name = longName,
                    color = "#FF0000",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("name", exception.errors!![0].field)
            assertEquals("Tag name is required and must be between 1 and 50 characters", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when color is not hex format`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "red",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("color", exception.errors!![0].field)
            assertEquals("Color must be a valid hex color (e.g., #FF0000)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when color is missing hash`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "FF0000",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("color", exception.errors!![0].field)
            assertEquals("Color must be a valid hex color (e.g., #FF0000)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when color is too short`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "#FF00",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("color", exception.errors!![0].field)
            assertEquals("Color must be a valid hex color (e.g., #FF0000)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when color is too long`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "#FF000000",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("color", exception.errors!![0].field)
            assertEquals("Color must be a valid hex color (e.g., #FF0000)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException when color contains invalid characters`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Important",
                    color = "#FF00GG",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(1, exception.errors!!.size)
            assertEquals("color", exception.errors!![0].field)
            assertEquals("Color must be a valid hex color (e.g., #FF0000)", exception.errors!![0].message)
        }

    @Test
    fun `toDomain should throw ValidationException with multiple errors`() =
        runTest {
            val longName = "a".repeat(51)
            val request =
                TagCreationRequest(
                    name = longName,
                    color = "invalid-color",
                )

            val exception =
                assertThrows<ValidationException> {
                    request.toDomain()
                }

            assertEquals("Validation failed", exception.message)
            assertEquals(2, exception.errors!!.size)

            val errorFields = exception.errors!!.map { it.field }.toSet()
            assertEquals(setOf("name", "color"), errorFields)
        }

    @Test
    fun `toDomain should accept valid name formats`() =
        runTest {
            val validNames =
                listOf(
                    "Important",
                    "Urgent",
                    "Work",
                    "Personal",
                    "Shopping",
                    "a".repeat(50), // Maximum length
                )

            validNames.forEach { name ->
                val request =
                    TagCreationRequest(
                        name = name,
                        color = "#FF0000",
                    )

                val domainRequest = request.toDomain()
                assertEquals(name, domainRequest.name)
            }
        }

    @Test
    fun `toDomain should accept valid hex color formats`() =
        runTest {
            val validColors =
                listOf(
                    "#FF0000", // Red
                    "#00FF00", // Green
                    "#0000FF", // Blue
                    "#FFFF00", // Yellow
                    "#FF00FF", // Magenta
                    "#00FFFF", // Cyan
                    "#000000", // Black
                    "#FFFFFF", // White
                    "#123456", // Mixed
                    "#ABCDEF", // Uppercase
                    "#abcdef", // Lowercase
                )

            validColors.forEach { color ->
                val request =
                    TagCreationRequest(
                        name = "Test Tag",
                        color = color,
                    )

                val domainRequest = request.toDomain()
                assertEquals(color.uppercase(), domainRequest.color)
            }
        }

    @Test
    fun `toDomain should handle name trimming correctly`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "  Test Tag  ",
                    color = "#FF0000",
                )

            val domainRequest = request.toDomain()
            assertEquals("Test Tag", domainRequest.name)
        }

    @Test
    fun `toDomain should handle color case conversion correctly`() =
        runTest {
            val request =
                TagCreationRequest(
                    name = "Test Tag",
                    color = "#ff0000",
                )

            val domainRequest = request.toDomain()
            assertEquals("#FF0000", domainRequest.color)
        }
}
