package com.ohana.domain.task

import com.ohana.TestUtils
import com.ohana.data.household.HouseholdRepository
import com.ohana.data.household.TagRepository
import com.ohana.data.task.TaskRepository
import com.ohana.data.task.TaskTagRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskTagManagerTest {
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var taskTagRepository: TaskTagRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var taskTagManager: TaskTagManager

    @BeforeEach
    fun setUp() {
        taskRepository = mock()
        tagRepository = mock()
        taskTagRepository = mock()
        householdRepository = mock()
        context =
            mock {
                on { tasks } doReturn taskRepository
                on { tags } doReturn tagRepository
                on { taskTags } doReturn taskTagRepository
                on { households } doReturn householdRepository
            }
        taskTagManager = TaskTagManager()
    }

    @Test
    fun `assignTagsToTask should create task tags when valid tags are provided`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tagIds =
                listOf(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                )

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                )

            val tags =
                listOf(
                    TestUtils.getTag(
                        id = tagIds[0],
                        householdId = householdId,
                    ),
                    TestUtils.getTag(
                        id = tagIds[1],
                        householdId = householdId,
                    ),
                )

            val expectedTaskTags =
                listOf(
                    TestUtils.getTaskTag(taskId = taskId, tagId = tagIds[0]),
                    TestUtils.getTaskTag(taskId = taskId, tagId = tagIds[1]),
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(tagRepository.findByIds(tagIds)).thenReturn(tags)
            whenever(taskTagRepository.deleteByTaskId(taskId)).thenReturn(true)
            whenever(taskTagRepository.createMany(any())).thenReturn(expectedTaskTags)

            val result = taskTagManager.assignTagsToTask(context, taskId, tagIds)

            assertEquals(tags, result)
            verify(taskTagRepository).deleteByTaskId(taskId)
            verify(taskTagRepository).createMany(any())
        }

    @Test
    fun `assignTagsToTask should return empty list when no tag IDs provided`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val tagIds = emptyList<String>()

            val result = taskTagManager.assignTagsToTask(context, taskId, tagIds)

            assertTrue(result.isEmpty())
            verify(taskTagRepository).deleteByTaskId(taskId)
            verify(taskTagRepository, never()).createMany(any())
        }

    @Test
    fun `assignTagsToTask should throw ValidationException when task not found`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val tagIds = listOf(UUID.randomUUID().toString())

            whenever(taskRepository.findById(taskId)).thenReturn(null)

            val ex =
                assertThrows<ValidationException> {
                    taskTagManager.assignTagsToTask(context, taskId, tagIds)
                }

            assertEquals("Task with ID $taskId not found", ex.message)
            verify(taskTagRepository).deleteByTaskId(taskId)
            verify(taskTagRepository, never()).createMany(any())
        }

    @Test
    fun `assignTagsToTask should throw ValidationException when tag not found`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()
            val tagId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = householdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(tagRepository.findByIds(listOf(tagId))).thenReturn(emptyList())

            val ex =
                assertThrows<ValidationException> {
                    taskTagManager.assignTagsToTask(context, taskId, listOf(tagId))
                }

            assertEquals("Tag $tagId not found", ex.message)
            verify(taskTagRepository).deleteByTaskId(taskId)
            verify(taskTagRepository, never()).createMany(any())
        }

    @Test
    fun `assignTagsToTask should throw ValidationException when tag belongs to different household`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val taskHouseholdId = UUID.randomUUID().toString()
            val tagHouseholdId = UUID.randomUUID().toString()
            val tagId = UUID.randomUUID().toString()

            val task =
                TestUtils.getTask(
                    id = taskId,
                    householdId = taskHouseholdId,
                )

            val tag =
                TestUtils.getTag(
                    id = tagId,
                    householdId = tagHouseholdId,
                )

            whenever(taskRepository.findById(taskId)).thenReturn(task)
            whenever(tagRepository.findByIds(listOf(tagId))).thenReturn(listOf(tag))

            val ex =
                assertThrows<ValidationException> {
                    taskTagManager.assignTagsToTask(context, taskId, listOf(tagId))
                }

            assertEquals("Tag $tagId does not belong to the same household as task $taskId", ex.message)
            verify(taskTagRepository).deleteByTaskId(taskId)
            verify(taskTagRepository, never()).createMany(any())
        }

    @Test
    fun `getTaskTags should return empty list when no task tags exist`() =
        runTest {
            val taskId = UUID.randomUUID().toString()

            whenever(taskTagRepository.findByTaskId(taskId)).thenReturn(emptyList())
            whenever(tagRepository.findByIds(emptyList())).thenReturn(emptyList())

            val result = taskTagManager.getTaskTags(context, taskId)

            assertTrue(result.isEmpty())
            verify(taskTagRepository).findByTaskId(taskId)
            verify(tagRepository).findByIds(emptyList())
        }

    @Test
    fun `getTaskTags should return tag info when task tags exist`() =
        runTest {
            val taskId = UUID.randomUUID().toString()
            val tagId = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val taskTag =
                TestUtils.getTaskTag(
                    taskId = taskId,
                    tagId = tagId,
                )

            val tag =
                TestUtils.getTag(
                    id = tagId,
                    name = "Test Tag",
                    color = "#3B82F6",
                    householdId = householdId,
                )

            whenever(taskTagRepository.findByTaskId(taskId)).thenReturn(listOf(taskTag))
            whenever(tagRepository.findByIds(listOf(tagId))).thenReturn(listOf(tag))

            val result = taskTagManager.getTaskTags(context, taskId)

            assertEquals(1, result.size)
            assertEquals(tag.id, result[0].id)
            assertEquals(tag.name, result[0].name)
            assertEquals(tag.color, result[0].color)
        }

    @Test
    fun `getTasksTags should return empty map when no task IDs provided`() =
        runTest {
            val taskIds = emptyList<String>()

            val result = taskTagManager.getTasksTags(context, taskIds)

            assertTrue(result.isEmpty())
            verify(taskTagRepository, never()).findByTaskIds(any())
        }

    @Test
    fun `getTasksTags should return empty lists when no task tags exist`() =
        runTest {
            val taskIds =
                listOf(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                )

            whenever(taskTagRepository.findByTaskIds(taskIds)).thenReturn(emptyList())
            whenever(tagRepository.findByIds(emptyList())).thenReturn(emptyList())

            val result = taskTagManager.getTasksTags(context, taskIds)

            assertEquals(2, result.size)
            assertTrue(result[taskIds[0]]!!.isEmpty())
            assertTrue(result[taskIds[1]]!!.isEmpty())
        }

    @Test
    fun `getTasksTags should return tag info for multiple tasks`() =
        runTest {
            val taskId1 = UUID.randomUUID().toString()
            val taskId2 = UUID.randomUUID().toString()
            val tagId1 = UUID.randomUUID().toString()
            val tagId2 = UUID.randomUUID().toString()
            val householdId = UUID.randomUUID().toString()

            val taskTags =
                listOf(
                    TestUtils.getTaskTag(taskId = taskId1, tagId = tagId1),
                    TestUtils.getTaskTag(taskId = taskId2, tagId = tagId2),
                )

            val tags =
                listOf(
                    TestUtils.getTag(
                        id = tagId1,
                        name = "Tag 1",
                        color = "#3B82F6",
                        householdId = householdId,
                    ),
                    TestUtils.getTag(
                        id = tagId2,
                        name = "Tag 2",
                        color = "#EF4444",
                        householdId = householdId,
                    ),
                )

            whenever(taskTagRepository.findByTaskIds(listOf(taskId1, taskId2))).thenReturn(taskTags)
            whenever(tagRepository.findByIds(listOf(tagId1, tagId2))).thenReturn(tags)

            val result = taskTagManager.getTasksTags(context, listOf(taskId1, taskId2))

            assertEquals(2, result.size)
            assertEquals(1, result[taskId1]!!.size)
            assertEquals(1, result[taskId2]!!.size)
            assertEquals("Tag 1", result[taskId1]!![0].name)
            assertEquals("Tag 2", result[taskId2]!![0].name)
            verify(tagRepository).findByIds(listOf(tagId1, tagId2))
        }
}
