package com.ohana.domain.tags

import com.ohana.TestUtils
import com.ohana.data.tags.*
import com.ohana.data.unitOfWork.*
import com.ohana.domain.tags.TaskTagManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class TagPermissionManagerTest {
    private lateinit var taskTagManager: TaskTagManager
    private lateinit var context: UnitOfWorkContext
    private lateinit var permissionRepository: PermissionRepository
    private lateinit var tagPermissionRepository: TagPermissionRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var manager: TagPermissionManager

    private val householdMemberId = UUID.randomUUID().toString()
    private val permissionId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        taskTagManager = mock()
        context = mock()
        permissionRepository = mock()
        tagPermissionRepository = mock()
        tagRepository = mock()

        whenever(context.permissions).thenReturn(permissionRepository)
        whenever(context.tagPermissions).thenReturn(tagPermissionRepository)
        whenever(context.tags).thenReturn(tagRepository)

        manager = TagPermissionManager(taskTagManager)
    }

    @Test
    fun `filterTasksByTagPermissions should return empty list when no tasks provided`() =
        runTest {
            // Given
            val taskIds = emptyList<String>()

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(emptyList(), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return empty list when no permission exists`() =
        runTest {
            // Given
            val taskIds = listOf("task1", "task2", "task3")

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(emptyList(), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return tasks with viewable tags`() =
        runTest {
            // Given
            val taskIds = listOf("task1", "task2", "task3")
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permissionId,
                        tagId = "tag1",
                        createdAt = Instant.now(),
                    ),
                )
            val taskTagsMap =
                mapOf(
                    "task1" to listOf(TestUtils.getTag(id = "tag1")),
                    "task2" to listOf(TestUtils.getTag(id = "tag2")),
                    "task3" to emptyList<Tag>(),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(taskTagManager.getTasksTags(context, taskIds)).thenReturn(taskTagsMap)

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(listOf("task1", "task3"), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return only tasks with permitted tags`() =
        runTest {
            // Given
            val taskIds = listOf("task1", "task2")
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permissionId,
                        tagId = "tag1",
                        createdAt = Instant.now(),
                    ),
                )
            val taskTagsMap =
                mapOf(
                    "task1" to listOf(TestUtils.getTag(id = "tag1")),
                    "task2" to listOf(TestUtils.getTag(id = "tag2")),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(taskTagManager.getTasksTags(context, taskIds)).thenReturn(taskTagsMap)

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(listOf("task1"), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return tasks with no tags`() =
        runTest {
            // Given
            val taskIds = listOf("task1", "task2")
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions = emptyList<TagPermission>()
            val taskTagsMap =
                mapOf(
                    "task1" to emptyList<Tag>(),
                    "task2" to listOf(TestUtils.getTag(id = "tag1")),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(taskTagManager.getTasksTags(context, taskIds)).thenReturn(taskTagsMap)

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(listOf("task1"), result)
        }

    @Test
    fun `filterTasksByTagPermissions should return only untagged tasks when user has no tag permissions`() =
        runTest {
            // Given
            val taskIds = listOf("task1", "task2", "task3")
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions = emptyList<TagPermission>()
            val taskTagsMap =
                mapOf(
                    "task1" to emptyList<Tag>(),
                    "task2" to listOf(TestUtils.getTag(id = "tag1")),
                    "task3" to listOf(TestUtils.getTag(id = "tag2")),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(taskTagManager.getTasksTags(context, taskIds)).thenReturn(taskTagsMap)

            // When
            val result = manager.filterTasksByTagPermissions(context, householdMemberId, taskIds)

            // Then
            assertEquals(listOf("task1"), result)
        }

    @Test
    fun `getUserViewableTags should return empty list when no permission exists`() =
        runTest {
            // Given
            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(null)

            // When
            val result = manager.getUserViewableTags(context, householdMemberId)

            // Then
            assertEquals(emptyList(), result)
        }

    @Test
    fun `getUserViewableTags should return user viewable tags when permission exists`() =
        runTest {
            // Given
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions =
                listOf(
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permissionId,
                        tagId = "tag1",
                        createdAt = Instant.now(),
                    ),
                    TagPermission(
                        id = UUID.randomUUID().toString(),
                        permissionId = permissionId,
                        tagId = "tag2",
                        createdAt = Instant.now(),
                    ),
                )
            val viewableTags =
                listOf(
                    TestUtils.getTag(id = "tag1", name = "Tag 1"),
                    TestUtils.getTag(id = "tag2", name = "Tag 2"),
                )

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(tagRepository.findByIds(listOf("tag1", "tag2"))).thenReturn(viewableTags)

            // When
            val result = manager.getUserViewableTags(context, householdMemberId)

            // Then
            assertEquals(2, result.size)
            assertEquals("tag1", result[0].id)
            assertEquals("tag2", result[1].id)
        }

    @Test
    fun `getUserViewableTags should return empty list when user has no tag permissions`() =
        runTest {
            // Given
            val permission =
                Permission(
                    id = permissionId,
                    householdMemberId = householdMemberId,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            val tagPermissions = emptyList<TagPermission>()

            whenever(permissionRepository.findByHouseholdMemberId(householdMemberId)).thenReturn(permission)
            whenever(tagPermissionRepository.findByPermissionId(permissionId)).thenReturn(tagPermissions)
            whenever(tagRepository.findByIds(emptyList())).thenReturn(emptyList())

            // When
            val result = manager.getUserViewableTags(context, householdMemberId)

            // Then
            assertEquals(emptyList(), result)
        }
}
