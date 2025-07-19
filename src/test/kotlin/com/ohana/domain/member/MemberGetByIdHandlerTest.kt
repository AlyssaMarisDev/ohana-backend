package com.ohana.domain.member

import com.ohana.TestUtils
import com.ohana.data.member.MemberRepository
import com.ohana.data.unitOfWork.*
import com.ohana.shared.exceptions.AuthorizationException
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
    fun `handle should return member when found and user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user accessing their own data

            val member =
                TestUtils.getMember(
                    id = memberId,
                    name = "Test User",
                    email = "test@example.com",
                    age = 25,
                    gender = "Male",
                )

            whenever(memberRepository.findById(memberId)).thenReturn(member)

            val response = handler.handle(userId, memberId)

            assertEquals(member.id, response.id)
            assertEquals(member.name, response.name)
            assertEquals(member.age, response.age)
            assertEquals(member.gender, response.gender)
            assertEquals(member.email, response.email)

            verify(memberRepository).findById(memberId)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should return member with null optional fields when user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user accessing their own data

            val member =
                TestUtils.getMember(
                    id = memberId,
                    name = "Test User",
                    email = "test@example.com",
                    age = null,
                    gender = null,
                )

            whenever(memberRepository.findById(memberId)).thenReturn(member)

            val response = handler.handle(userId, memberId)

            assertEquals(member.id, response.id)
            assertEquals(member.name, response.name)
            assertEquals(null, response.age)
            assertEquals(null, response.gender)
            assertEquals(member.email, response.email)

            verify(memberRepository).findById(memberId)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user tries to access different member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString() // Different user

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, memberId)
                }
            assertEquals("You can only access your own member information", ex.message)
            verifyNoInteractions(memberRepository)
        }

    @Test
    fun `handle should throw NotFoundException when member does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user accessing their own data

            whenever(memberRepository.findById(memberId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, memberId)
                }
            assertEquals("Member not found", ex.message)
            verify(memberRepository).findById(memberId)
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should propagate exception from repository`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user accessing their own data

            whenever(memberRepository.findById(memberId)).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, memberId)
                }
            assertEquals("DB error", ex.message)
            verify(memberRepository).findById(memberId)
            verifyNoMoreInteractions(memberRepository)
        }
}
