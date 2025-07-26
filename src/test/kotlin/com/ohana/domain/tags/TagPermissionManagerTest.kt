package com.ohana.domain.tags

import com.ohana.data.tags.Permission
import com.ohana.data.tags.Tag
import com.ohana.data.tags.TagPermission
import com.ohana.data.unitOfWork.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class TagPermissionManagerTest {
    private lateinit var tagPermissionManager: TagPermissionManager
    private lateinit var mockTaskTagManager: TaskTagManager
    private lateinit var mockContext: UnitOfWorkContext

    @BeforeEach
    fun setUp() {
        mockTaskTagManager = mock()
        tagPermissionManager = TagPermissionManager(mockTaskTagManager)
        mockContext =
            mock {
                on { permissions } doReturn mock()
                on { tagPermissions } doReturn mock()
            }
    }

    @Test
    fun `filterTasksByTagPermissions should return all tasks when no permission exists`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2", "task3")

            whenever(mockContext.permissions.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

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
                Permission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = "allowed-tag",
                        createdAt = Instant.now(),
                    ),
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = "another-allowed-tag",
                        createdAt = Instant.now(),
                    ),
                )

            val taskTagsMap =
                mapOf(
                    "task1" to
                        listOf(
                            Tag(
                                "allowed-tag",
                                "Allowed Tag",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task2" to
                        listOf(
                            Tag(
                                "excluded-tag",
                                "Excluded Tag",
                                "#000000",
                                "household1",
                                false,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task3" to
                        listOf(
                            Tag(
                                "another-allowed-tag",
                                "Another Allowed Tag",
                                "#000000",
                                "household1",
                                false,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                )

            whenever(mockContext.permissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.tagPermissions.findByPermissionId(permission.id)).thenReturn(tagPermissions)
            whenever(mockTaskTagManager.getTasksTags(mockContext, taskIds)).thenReturn(taskTagsMap)

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
                Permission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = "allowed-tag",
                        createdAt = Instant.now(),
                    ),
                )

            val taskTagsMap =
                mapOf(
                    "task1" to
                        listOf(
                            Tag(
                                "allowed-tag",
                                "Allowed Tag",
                                "#000000",
                                "household1",
                                false,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task2" to emptyList<Tag>(),
                )

            whenever(mockContext.permissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.tagPermissions.findByPermissionId(permission.id)).thenReturn(tagPermissions)
            whenever(mockTaskTagManager.getTasksTags(mockContext, taskIds)).thenReturn(taskTagsMap)

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

    @Test
    fun `filterTasksByTagPermissions should return empty list when no tasks provided`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = emptyList<String>()

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `filterTasksByTagPermissions should return only tasks with permitted tags`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2", "task3", "task4")

            val permission =
                Permission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = "kids",
                        createdAt = Instant.now(),
                    ),
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permission.id,
                        tagId = "chores",
                        createdAt = Instant.now(),
                    ),
                )

            val taskTagsMap =
                mapOf(
                    "task1" to
                        listOf(
                            Tag(
                                "kids",
                                "Kids",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task2" to
                        listOf(
                            Tag(
                                "work",
                                "Work",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task3" to
                        listOf(
                            Tag(
                                "chores",
                                "Chores",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task4" to emptyList<Tag>(),
                )

            whenever(mockContext.permissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.tagPermissions.findByPermissionId(permission.id)).thenReturn(tagPermissions)
            whenever(mockTaskTagManager.getTasksTags(mockContext, taskIds)).thenReturn(taskTagsMap)

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertEquals(listOf("task1", "task3", "task4"), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return only untagged tasks when user has no tag permissions`() =
        runTest {
            // Given
            val householdMemberId = UUID.randomUUID().toString()
            val taskIds = listOf("task1", "task2", "task3")

            val permission =
                Permission(
                    id = UUID.randomUUID().toString(),
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            val taskTagsMap =
                mapOf(
                    "task1" to
                        listOf(
                            Tag(
                                "kids",
                                "Kids",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                    "task2" to emptyList<Tag>(),
                    "task3" to
                        listOf(
                            Tag(
                                "work",
                                "Work",
                                "#000000",
                                "household1",
                                true,
                                Instant.now(),
                                Instant.now(),
                            ),
                        ),
                )

            whenever(mockContext.permissions.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(mockContext.tagPermissions.findByPermissionId(permission.id)).thenReturn(emptyList())
            whenever(mockTaskTagManager.getTasksTags(mockContext, taskIds)).thenReturn(taskTagsMap)

            // When
            val result =
                tagPermissionManager.filterTasksByTagPermissions(
                    context = mockContext,
                    householdMemberId = householdMemberId,
                    taskIds = taskIds,
                )

            // Then
            assertEquals(listOf("task2"), result)
        }
}
