package com.ohana.domain.member

import com.ohana.TestUtils
import com.ohana.data.member.MemberRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.NotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals

class MemberGetByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberGetByIdHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context =
            mock {
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = MemberGetByIdHandler(unitOfWork)
    }

    @Test
    fun `handle should return member when found`() =
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

            whenever(memberRepository.findById(member.id)).thenReturn(member)

            val response = handler.handle(member.id)

            assertEquals(member.id, response.id)
            assertEquals(member.name, response.name)
            assertEquals(member.age, response.age)
            assertEquals(member.gender, response.gender)
            assertEquals(member.email, response.email)

            verify(memberRepository).findById(member.id)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return member with null optional fields`() =
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

            whenever(memberRepository.findById(member.id)).thenReturn(member)

            val response = handler.handle(member.id)

            assertEquals(member.id, response.id)
            assertEquals(member.name, response.name)
            assertEquals(null, response.age)
            assertEquals(null, response.gender)
            assertEquals(member.email, response.email)

            verify(memberRepository).findById(member.id)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should throw NotFoundException when member does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val id = UUID.randomUUID().toString()
            whenever(memberRepository.findById(id)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(id)
                }
            assertEquals("Member not found", ex.message)
            verify(memberRepository).findById(id)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val id = UUID.randomUUID().toString()
            whenever(memberRepository.findById(id)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(id)
                }
            assertEquals("DB error", ex.message)
            verify(memberRepository).findById(id)
            verifyNoMoreInteractions(memberRepository)
        }
}
