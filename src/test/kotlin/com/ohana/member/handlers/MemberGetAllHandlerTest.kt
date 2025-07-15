package com.ohana.member.handlers

import com.ohana.TestUtils
import com.ohana.shared.MemberRepository
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberGetAllHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberGetAllHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context =
            mock {
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = MemberGetAllHandler(unitOfWork)
    }

    @Test
    fun `handle should return empty list when no members exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(memberRepository.findAll()).thenReturn(emptyList())

            val response = handler.handle()

            assertTrue(response.isEmpty())
            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return single member when one exists`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val member =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = "test@example.com",
                    age = 25,
                    gender = "Male",
                )

            whenever(memberRepository.findAll()).thenReturn(listOf(member))

            val response = handler.handle()

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(member.id, responseItem.id)
            assertEquals(member.name, responseItem.name)
            assertEquals(member.age, responseItem.age)
            assertEquals(member.gender, responseItem.gender)
            assertEquals(member.email, responseItem.email)

            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return multiple members when multiple exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val member1 =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "First User",
                    email = "first@example.com",
                    age = 25,
                    gender = "Male",
                )

            val member2 =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "Second User",
                    email = "second@example.com",
                    age = 30,
                    gender = "Female",
                )

            val member3 =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "Third User",
                    email = "third@example.com",
                    age = null,
                    gender = null,
                )

            whenever(memberRepository.findAll()).thenReturn(listOf(member1, member2, member3))

            val response = handler.handle()

            assertEquals(3, response.size)

            // Verify first member
            val response1 = response[0]
            assertEquals(member1.id, response1.id)
            assertEquals(member1.name, response1.name)
            assertEquals(member1.age, response1.age)
            assertEquals(member1.gender, response1.gender)
            assertEquals(member1.email, response1.email)

            // Verify second member
            val response2 = response[1]
            assertEquals(member2.id, response2.id)
            assertEquals(member2.name, response2.name)
            assertEquals(member2.age, response2.age)
            assertEquals(member2.gender, response2.gender)
            assertEquals(member2.email, response2.email)

            // Verify third member
            val response3 = response[2]
            assertEquals(member3.id, response3.id)
            assertEquals(member3.name, response3.name)
            assertEquals(null, response3.age)
            assertEquals(null, response3.gender)
            assertEquals(member3.email, response3.email)

            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return members with null optional fields`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val member =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = "test@example.com",
                    age = null,
                    gender = null,
                )

            whenever(memberRepository.findAll()).thenReturn(listOf(member))

            val response = handler.handle()

            assertEquals(1, response.size)
            val responseItem = response.first()
            assertEquals(member.id, responseItem.id)
            assertEquals(member.name, responseItem.name)
            assertEquals(null, responseItem.age)
            assertEquals(null, responseItem.gender)
            assertEquals(member.email, responseItem.email)

            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should maintain order of members from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val member1 =
                TestUtils.getMember(
                    id = "1",
                    name = "First",
                    email = "first@example.com",
                )

            val member2 =
                TestUtils.getMember(
                    id = "2",
                    name = "Second",
                    email = "second@example.com",
                )

            val member3 =
                TestUtils.getMember(
                    id = "3",
                    name = "Third",
                    email = "third@example.com",
                )

            val members = listOf(member1, member2, member3)
            whenever(memberRepository.findAll()).thenReturn(members)

            val response = handler.handle()

            assertEquals(3, response.size)
            assertEquals("1", response[0].id)
            assertEquals("2", response[1].id)
            assertEquals("3", response[2].id)

            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            whenever(memberRepository.findAll()).thenThrow(RuntimeException("Database connection failed"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle()
                }

            assertEquals("Database connection failed", ex.message)
            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return correct response structure`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val member =
                TestUtils.getMember(
                    id = UUID.randomUUID().toString(),
                    name = "Test User",
                    email = "test@example.com",
                    age = 25,
                    gender = "Male",
                )

            whenever(memberRepository.findAll()).thenReturn(listOf(member))

            val response = handler.handle()

            assertEquals(1, response.size)
            val responseItem = response.first()

            // Verify property values
            assertEquals(member.id, responseItem.id)
            assertEquals(member.name, responseItem.name)
            assertEquals(member.age, responseItem.age)
            assertEquals(member.gender, responseItem.gender)
            assertEquals(member.email, responseItem.email)

            verify(memberRepository).findAll()
            verifyNoMoreInteractions(memberRepository)
        }
}
