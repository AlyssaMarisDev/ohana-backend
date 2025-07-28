package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.permissions.PermissionRepository
import com.ohana.data.permissions.TagPermissionRepository
import com.ohana.data.tags.TagRepository
import com.ohana.data.unitOfWork.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.TaskTagManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class GetTagsHandlerTest {
    private lateinit var tagRepository: TagRepository
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var tagPermissionRepository: TagPermissionRepository
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var context: UnitOfWorkContext
    private lateinit var handler: GetTagsHandler

    @BeforeEach
    fun setUp() {
        tagRepository = mock()
        permissionRepository = mock()
        tagPermissionRepository = mock()
        taskTagManager = mock()
        tagPermissionManager = mock()
        context = mock()
        handler = GetTagsHandler(tagRepository, tagPermissionManager, taskTagManager)
    }

    @Test
    fun `handle should return empty list when no permission exists`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val request = GetTagsHandler.Request(householdMemberId)

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(emptyList(), response.tags)
        }

    @Test
    fun `handle should return user viewable tags when permission exists`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val permissionId = UUID.randomUUID().toString()
            val request = GetTagsHandler.Request(householdMemberId)

            val permission =
                TestUtils.getPermission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                )

            val viewableTags =
                listOf(
                    TestUtils.getTag(id = "tag1", name = "Tag 1"),
                    TestUtils.getTag(id = "tag2", name = "Tag 2"),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionManager.getUserViewableTags(context, householdMemberId)).thenReturn(viewableTags)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(2, response.tags.size)
            assertEquals("tag1", response.tags[0].id)
            assertEquals("tag2", response.tags[1].id)
        }

    @Test
    fun `handle should return empty list when user has no tag permissions`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val permissionId = UUID.randomUUID().toString()
            val request = GetTagsHandler.Request(householdMemberId)

            val permission =
                TestUtils.getPermission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionManager.getUserViewableTags(context, householdMemberId)).thenReturn(emptyList())

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(emptyList(), response.tags)
        }

    @Test
    fun `handle should throw exception when household member ID is invalid`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val request = GetTagsHandler.Request("invalid-id")

            whenever(permissionRepository.findByHouseholdMemberId("invalid-id")).thenThrow(RuntimeException("Invalid ID"))

            // When & Then
            assertThrows<RuntimeException> {
                handler.handle(userId, request)
            }
        }

    @Test
    fun `handle should return tags with correct response structure`() =
        runTest {
            // Given
            val userId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val permissionId = UUID.randomUUID().toString()
            val request = GetTagsHandler.Request(householdMemberId)

            val permission =
                TestUtils.getPermission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                )

            val viewableTags =
                listOf(
                    TestUtils.getTag(
                        id = "tag1",
                        name = "Default Tag",
                        color = "#FF0000",
                        isDefault = true,
                    ),
                    TestUtils.getTag(
                        id = "tag2",
                        name = "Household Tag",
                        color = "#00FF00",
                        householdId = "household1",
                        isDefault = false,
                    ),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionManager.getUserViewableTags(context, householdMemberId)).thenReturn(viewableTags)

            // When
            val response = handler.handle(userId, request)

            // Then
            assertEquals(2, response.tags.size)

            val firstTag = response.tags[0]
            assertEquals("tag1", firstTag.id)
            assertEquals("Default Tag", firstTag.name)
            assertEquals("#FF0000", firstTag.color)
            assertEquals(true, firstTag.isDefault)
            assertEquals(null, firstTag.householdId)

            val secondTag = response.tags[1]
            assertEquals("tag2", secondTag.id)
            assertEquals("Household Tag", secondTag.name)
            assertEquals("#00FF00", secondTag.color)
            assertEquals(false, secondTag.isDefault)
            assertEquals("household1", secondTag.householdId)
        }
}
