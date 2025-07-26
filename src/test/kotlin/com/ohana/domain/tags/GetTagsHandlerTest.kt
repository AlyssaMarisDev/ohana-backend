package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.tags.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.validators.HouseholdMemberValidator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class GetTagsHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var tagPermissionRepository: TagPermissionRepository
    private lateinit var validator: HouseholdMemberValidator
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var handler: GetTagsHandler

    private val userId = UUID.randomUUID().toString()
    private val householdId = UUID.randomUUID().toString()
    private val memberId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        unitOfWork = mock()
        context = mock()
        householdRepository = mock()
        tagRepository = mock()
        permissionRepository = mock()
        tagPermissionRepository = mock()
        validator = mock()
        tagPermissionManager = mock()

        whenever(context.households).thenReturn(householdRepository)
        whenever(context.tags).thenReturn(tagRepository)
        whenever(context.permissions).thenReturn(permissionRepository)
        whenever(context.tagPermissions).thenReturn(tagPermissionRepository)

        handler = GetTagsHandler(unitOfWork, validator, tagPermissionManager)
    }

    @Test
    fun `handle should return user viewable tags for household`() =
        runTest {
            // Given
            val request = GetTagsHandler.Request(householdId = householdId)
            val viewableTags =
                listOf(
                    TestUtils.getTag(id = "tag1", name = "Household Tag 1", householdId = householdId),
                    TestUtils.getTag(id = "tag2", name = "Household Tag 2", householdId = householdId),
                )

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(validator.validate(context, householdId, userId)).thenReturn(memberId)
            whenever(tagPermissionManager.getUserViewableTags(context, memberId)).thenReturn(viewableTags)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(2, response.tags.size)
            assertEquals("tag1", response.tags[0].id)
            assertEquals("Household Tag 1", response.tags[0].name)
            assertEquals("tag2", response.tags[1].id)
            assertEquals("Household Tag 2", response.tags[1].name)
            verify(validator).validate(context, householdId, userId)
            verify(tagPermissionManager).getUserViewableTags(context, memberId)
        }

    @Test
    fun `handle should return empty list when user has no viewable tags`() =
        runTest {
            // Given
            val request = GetTagsHandler.Request(householdId = householdId)

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(validator.validate(context, householdId, userId)).thenReturn(memberId)
            whenever(tagPermissionManager.getUserViewableTags(context, memberId)).thenReturn(emptyList())

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(0, response.tags.size)
            verify(validator).validate(context, householdId, userId)
            verify(tagPermissionManager).getUserViewableTags(context, memberId)
        }

    @Test
    fun `handle should propagate validation exception`() =
        runTest {
            // Given
            val request = GetTagsHandler.Request(householdId = householdId)
            val exception =
                com.ohana.shared.exceptions
                    .AuthorizationException("User is not a member")

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(validator.validate(context, householdId, userId)).thenThrow(exception)

            // When & Then
            assertThrows<com.ohana.shared.exceptions.AuthorizationException> {
                handler.handle(userId, request)
            }
        }

    @Test
    fun `handle should propagate repository exception`() =
        runTest {
            // Given
            val request = GetTagsHandler.Request(householdId = householdId)
            val exception = RuntimeException("Database error")

            TestUtils.mockUnitOfWork(unitOfWork, context)
            whenever(validator.validate(context, householdId, userId)).thenReturn(memberId)
            whenever(tagPermissionManager.getUserViewableTags(context, memberId)).thenThrow(exception)

            // When & Then
            assertThrows<RuntimeException> {
                handler.handle(userId, request)
            }
        }
}
