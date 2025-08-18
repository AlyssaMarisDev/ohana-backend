package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.tags.TagRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.validators.HouseholdMemberValidator
import com.ohana.shared.exceptions.ConflictException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID

class TagCreationHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var tagRepository: TagRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var handler: TagCreationHandler

    @BeforeEach
    fun setUp() {
        tagRepository = mock()
        validator = mock()
        tagPermissionManager = mock()
        context =
            mock {
                on { tags } doReturn tagRepository
            }
        unitOfWork = mock()
        handler = TagCreationHandler(unitOfWork, validator, tagPermissionManager)
    }

    @Test
    fun `handle should create tag and return response when valid request provided`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val color = "#FF0000"

            val tag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenReturn(tag)

            val request = TagCreationHandler.Request(name, color)
            val response = handler.handle(userId, householdId, request)

            assertEquals(tag.id, response.id)
            assertEquals(name, response.name)
            assertEquals(color, response.color)
            assertEquals(householdId, response.householdId)
            assertNotNull(response.createdAt)
            assertNotNull(response.updatedAt)

            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
            verify(tagRepository).create(
                argThat { createdTag ->
                    createdTag.name == name &&
                        createdTag.color == color &&
                        createdTag.householdId == householdId
                },
            )
            verify(tagPermissionManager).giveTagPermissionsToMember(context, householdMemberId, listOf(tag.id))
            verifyNoMoreInteractions(tagRepository, tagPermissionManager)
        }

    @Test
    fun `handle should throw ConflictException when tag with same name already exists`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Existing Tag"
            val color = "#FF0000"

            val existingTag =
                TestUtils.getTag(
                    name = name,
                    color = "#00FF00", // Different color
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(listOf(existingTag))

            val request = TagCreationHandler.Request(name, color)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(userId, householdId, request)
                }

            assertEquals("A tag with the name '$name' already exists in this household", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
            verify(tagRepository, never()).create(any())
            verify(tagPermissionManager, never()).giveTagPermissionsToMember(any(), any(), any())
        }

    @Test
    fun `handle should work with case insensitive name comparison`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val existingName = "Existing Tag"
            val newName = "EXISTING TAG" // Same name, different case
            val color = "#FF0000"

            val existingTag =
                TestUtils.getTag(
                    name = existingName,
                    color = "#00FF00",
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(listOf(existingTag))

            val request = TagCreationHandler.Request(newName, color)

            val ex =
                assertThrows<ConflictException> {
                    handler.handle(userId, householdId, request)
                }

            assertEquals("A tag with the name '$newName' already exists in this household", ex.message)
        }

    @Test
    fun `handle should work with empty name`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = ""
            val color = "#FF0000"

            val tag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenReturn(tag)

            val request = TagCreationHandler.Request(name, color)
            val response = handler.handle(userId, householdId, request)

            assertEquals(name, response.name)
        }

    @Test
    fun `handle should work with special characters in name`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Tag with special chars: !@#\$%^&*()"
            val color = "#FF0000"

            val tag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenReturn(tag)

            val request = TagCreationHandler.Request(name, color)
            val response = handler.handle(userId, householdId, request)

            assertEquals(name, response.name)
        }

    @Test
    fun `handle should work with different color formats`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val colors = listOf("#FF0000", "#00FF00", "#0000FF", "red", "blue", "green")

            colors.forEach { color ->
                val tag =
                    TestUtils.getTag(
                        name = name,
                        color = color,
                        householdId = householdId,
                    )

                whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
                whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
                whenever(tagRepository.create(any())).thenReturn(tag)

                val request = TagCreationHandler.Request(name, color)
                val response = handler.handle(userId, householdId, request)

                assertEquals(color, response.color)
            }
        }

    @Test
    fun `handle should work when household has other tags with different names`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "New Tag"
            val color = "#FF0000"

            val existingTags =
                listOf(
                    TestUtils.getTag(name = "Other Tag 1", householdId = householdId),
                    TestUtils.getTag(name = "Other Tag 2", householdId = householdId),
                )

            val newTag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(existingTags)
            whenever(tagRepository.create(any())).thenReturn(newTag)

            val request = TagCreationHandler.Request(name, color)
            val response = handler.handle(userId, householdId, request)

            assertEquals(name, response.name)
            assertEquals(color, response.color)
        }

    @Test
    fun `handle should propagate exception from household member validator`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val color = "#FF0000"

            whenever(validator.validate(context, householdId, userId))
                .thenThrow(
                    com.ohana.shared.exceptions
                        .AuthorizationException("User not authorized"),
                )

            val request = TagCreationHandler.Request(name, color)

            val ex =
                assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                    handler.handle(userId, householdId, request)
                }

            assertEquals("User not authorized", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository, never()).findByHouseholdId(any())
            verify(tagRepository, never()).create(any())
            verify(tagPermissionManager, never()).giveTagPermissionsToMember(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from tag repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val color = "#FF0000"

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenThrow(RuntimeException("DB error"))

            val request = TagCreationHandler.Request(name, color)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId, request)
                }

            assertEquals("DB error", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
            verify(tagRepository).create(any())
            verify(tagPermissionManager, never()).giveTagPermissionsToMember(any(), any(), any())
        }

    @Test
    fun `handle should propagate exception from tag permission manager`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val color = "#FF0000"

            val tag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenReturn(tag)
            whenever(tagPermissionManager.giveTagPermissionsToMember(context, householdMemberId, listOf(tag.id)))
                .thenThrow(RuntimeException("Permission error"))

            val request = TagCreationHandler.Request(name, color)

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, householdId, request)
                }

            assertEquals("Permission error", ex.message)
            verify(validator).validate(context, householdId, userId)
            verify(tagRepository).findByHouseholdId(householdId)
            verify(tagRepository).create(any())
            verify(tagPermissionManager).giveTagPermissionsToMember(context, householdMemberId, listOf(tag.id))
        }

    @Test
    fun `handle should generate unique ID for tag`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val userId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val name = "Test Tag"
            val color = "#FF0000"

            val tag =
                TestUtils.getTag(
                    name = name,
                    color = color,
                    householdId = householdId,
                )

            whenever(validator.validate(context, householdId, userId)).thenReturn(householdMemberId)
            whenever(tagRepository.findByHouseholdId(householdId)).thenReturn(emptyList())
            whenever(tagRepository.create(any())).thenReturn(tag)

            val request = TagCreationHandler.Request(name, color)
            val response = handler.handle(userId, householdId, request)

            assertNotNull(response.id)
            assertTrue(response.id.isNotEmpty())
            assertNotEquals(householdId, response.id)
        }
}
