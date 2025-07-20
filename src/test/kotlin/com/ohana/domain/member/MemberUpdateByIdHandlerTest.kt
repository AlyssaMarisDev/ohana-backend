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

class MemberUpdateByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberUpdateByIdHandler

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context =
            mock {
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = MemberUpdateByIdHandler(unitOfWork)
    }

    @Test
    fun `handle should update member successfully when user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user updating their own data

            val existingMember =
                TestUtils.getMember(
                    id = memberId,
                    name = "Original Name",
                    email = "original@example.com",
                    age = 25,
                    gender = "Male",
                )
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )
            val updatedMember =
                existingMember.copy(
                    name = request.name,
                    age = request.age,
                    gender = request.gender,
                )

            whenever(memberRepository.findById(memberId)).thenReturn(existingMember)
            whenever(memberRepository.update(any())).thenReturn(updatedMember)

            val response = handler.handle(userId, memberId, request)

            assertEquals(memberId, response.id)
            assertEquals("Updated Name", response.name)
            assertEquals(30, response.age)
            assertEquals("Female", response.gender)
            assertEquals("original@example.com", response.email)

            verify(memberRepository).findById(memberId)
            verify(memberRepository).update(
                argThat { member ->
                    member.id == memberId &&
                        member.name == "Updated Name" &&
                        member.age == 30 &&
                        member.gender == "Female" &&
                        member.email == "original@example.com"
                },
            )
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should update member with null optional fields when user is authorized`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user updating their own data

            val existingMember =
                TestUtils.getMember(
                    id = memberId,
                    name = "Original Name",
                    email = "original@example.com",
                    age = 25,
                    gender = "Male",
                )
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = null,
                    gender = null,
                )
            val updatedMember =
                existingMember.copy(
                    name = request.name,
                    age = null,
                    gender = null,
                )

            whenever(memberRepository.findById(memberId)).thenReturn(existingMember)
            whenever(memberRepository.update(any())).thenReturn(updatedMember)

            val response = handler.handle(userId, memberId, request)

            assertEquals(memberId, response.id)
            assertEquals("Updated Name", response.name)
            assertEquals(null, response.age)
            assertEquals(null, response.gender)
            assertEquals("original@example.com", response.email)

            verify(memberRepository).findById(memberId)
            verify(memberRepository).update(
                argThat { member ->
                    member.id == memberId &&
                        member.name == "Updated Name" &&
                        member.age == null &&
                        member.gender == null &&
                        member.email == "original@example.com"
                },
            )
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should throw AuthorizationException when user tries to update different member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString() // Different user

            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )

            val ex =
                assertThrows<AuthorizationException> {
                    handler.handle(userId, memberId, request)
                }
            assertEquals("You can only update your own member information", ex.message)
            verifyNoInteractions(memberRepository)
        }

    @Test
    fun `handle should throw NotFoundException when member does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user updating their own data

            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )

            whenever(memberRepository.findById(memberId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(userId, memberId, request)
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
            val userId = memberId // Same user updating their own data

            val existingMember = TestUtils.getMember(id = memberId)
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )

            whenever(memberRepository.findById(memberId)).thenReturn(existingMember)
            whenever(memberRepository.update(any())).thenThrow(RuntimeException("DB error"))

            val ex =
                assertThrows<RuntimeException> {
                    handler.handle(userId, memberId, request)
                }
            assertEquals("DB error", ex.message)
            verify(memberRepository).findById(memberId)
            verify(memberRepository).update(any())
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should preserve email from existing member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val userId = memberId // Same user updating their own data

            val existingMember =
                TestUtils.getMember(
                    id = memberId,
                    name = "Original Name",
                    email = "preserved@example.com",
                    age = 25,
                    gender = "Male",
                )
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )
            val updatedMember =
                existingMember.copy(
                    name = request.name,
                    age = request.age,
                    gender = request.gender,
                )

            whenever(memberRepository.findById(memberId)).thenReturn(existingMember)
            whenever(memberRepository.update(any())).thenReturn(updatedMember)

            val response = handler.handle(userId, memberId, request)

            assertEquals("preserved@example.com", response.email)
            verify(memberRepository).update(
                argThat { member ->
                    member.email == "preserved@example.com"
                },
            )
        }
}
