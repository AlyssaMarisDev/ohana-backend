package com.ohana.domain.tags

import com.ohana.data.tags.TagPermission
import com.ohana.data.tags.TaskTag
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.shared.enums.TagPermissionType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TagPermissionManagerTest {
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var mockContext: UnitOfWorkContext

    @BeforeEach
    fun setUp() {
        tagPermissionManager = TagPermissionManager()
        mockContext =
            mock {
                on { tagPermissions } doReturn mock()
                on { taskTags } doReturn mock()
            }
    }

    @Test
    fun `setTagPermissions should create new permission when none exists`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val permissionType = TagPermissionType.ALLOW_ALL_EXCEPT
            val tagIds = listOf("tag1", "tag2")

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(null)
            whenever(mockContext.tagPermissions.create(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            // When
            val result =
                tagPermissionManager.setTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    permissionType = permissionType,
                    tagIds = tagIds,
                )

            // Then
            assertEquals(householdMemberId, result.householdMemberId)
            assertEquals(permissionType, result.permissionType)
            assertEquals(tagIds, result.tagIds)
            verify(mockContext.tagPermissions).create(any())
        }

    @Test
    fun `setTagPermissions should update existing permission when one exists`() =
        runTest {
            // Given
            val permissionId = UUID.randomUUID().toString()
            val householdMemberId = UUID.randomUUID().toString()
            val permissionType = TagPermissionType.DENY_ALL_EXCEPT
            val tagIds = listOf("tag3")

            val existingPermission =
                TagPermission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    permissionType = TagPermissionType.ALLOW_ALL_EXCEPT,
                    tagIds = listOf("old-tag"),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(existingPermission)
            whenever(mockContext.tagPermissions.update(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            // When
            val result =
                tagPermissionManager.setTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    permissionType = permissionType,
                    tagIds = tagIds,
                )

            // Then
            assertEquals(permissionId, result.id)
            assertEquals(permissionType, result.permissionType)
            assertEquals(tagIds, result.tagIds)
            verify(mockContext.tagPermissions).update(any())
        }

    @Test
    fun `filterTagsByPermissions should return all tags when no permission exists`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val allTagIds = listOf("tag1", "tag2", "tag3")

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

            // When
            val result =
                tagPermissionManager.filterTagsByPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    allTagIds = allTagIds,
                )

            // Then
            assertEquals(allTagIds, result)
        }

    @Test
    fun `filterTagsByPermissions should filter out excluded tags for ALLOW_ALL_EXCEPT`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val allTagIds = listOf("tag1", "tag2", "tag3", "tag4")
            val excludedTagIds = listOf("tag2", "tag4")

            val permission =
                TagPermission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    permissionType = TagPermissionType.ALLOW_ALL_EXCEPT,
                    tagIds = excludedTagIds,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)

            // When
            val result =
                tagPermissionManager.filterTagsByPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    allTagIds = allTagIds,
                )

            // Then
            assertEquals(listOf("tag1", "tag3"), result)
        }

    @Test
    fun `filterTagsByPermissions should only return allowed tags for DENY_ALL_EXCEPT`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val allTagIds = listOf("tag1", "tag2", "tag3", "tag4")
            val allowedTagIds = listOf("tag2", "tag4")

            val permission =
                TagPermission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    permissionType = TagPermissionType.DENY_ALL_EXCEPT,
                    tagIds = allowedTagIds,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)

            // When
            val result =
                tagPermissionManager.filterTagsByPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    allTagIds = allTagIds,
                )

            // Then
            assertEquals(allowedTagIds, result)
        }

    @Test
    fun `filterTasksByTagPermissions should return all tasks when no permission exists`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2", "task3")

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertEquals(taskIds, result)
        }

    @Test
    fun `filterTasksByTagPermissions should return tasks with viewable tags`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2", "task3")

            val permission =
                TagPermission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    permissionType = TagPermissionType.ALLOW_ALL_EXCEPT,
                    tagIds = listOf("excluded-tag"),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val taskTags =
                listOf(
                    TaskTag("tt1", "task1", "allowed-tag", Instant.now()),
                    TaskTag("tt2", "task2", "excluded-tag", Instant.now()),
                    TaskTag("tt3", "task3", "another-allowed-tag", Instant.now()),
                )

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.taskTags.findByTaskIds(taskIds)).thenReturn(taskTags)

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertEquals(listOf("task1", "task3"), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return tasks with no tags`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2")

            val permission =
                TagPermission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    permissionType = TagPermissionType.DENY_ALL_EXCEPT,
                    tagIds = listOf("allowed-tag"),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val taskTags =
                listOf(
                    TaskTag("tt1", "task1", "allowed-tag", Instant.now()),
                    // task2 has no tags
                )

            whenever(mockContext.tagPermissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.taskTags.findByTaskIds(taskIds)).thenReturn(taskTags)

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertEquals(listOf("task1", "task2"), result)
        }
}
