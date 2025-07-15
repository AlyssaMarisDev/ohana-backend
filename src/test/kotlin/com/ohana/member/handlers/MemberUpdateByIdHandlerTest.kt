package com.ohana.member.handlers

import com.ohana.TestUtils
import com.ohana.exceptions.NotFoundException
import com.ohana.shared.MemberRepository
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberUpdateByIdHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: MemberUpdateByIdHandler
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        memberRepository = mock()
        context =
            mock {
                on { members } doReturn memberRepository
            }
        unitOfWork = mock()
        handler = MemberUpdateByIdHandler(unitOfWork)
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `handle should update member successfully`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
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

            val response = handler.handle(memberId, request)

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
    fun `handle should update member with null optional fields`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
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

            val response = handler.handle(memberId, request)

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
    fun `handle should throw NotFoundException when member does not exist`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "Updated Name",
                    age = 30,
                    gender = "Female",
                )

            whenever(memberRepository.findById(memberId)).thenReturn(null)

            val ex =
                assertThrows<NotFoundException> {
                    handler.handle(memberId, request)
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
                    handler.handle(memberId, request)
                }
            assertEquals("DB error", ex.message)
            verify(memberRepository).findById(memberId)
            verify(memberRepository).update(any())
            verifyNoMoreInteractions(memberRepository)
        }

    @Test
    fun `handle should throw ValidationException when name is empty`() =
        runTest {
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "",
                    age = 30,
                    gender = "Female",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Name is required"))
        }

    @Test
    fun `handle should throw ValidationException when name is too long`() =
        runTest {
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "A".repeat(256), // 256 characters, exceeds 255 limit
                    age = 30,
                    gender = "Female",
                )
            val violations = validator.validate(request)
            val messages = violations.map { it.propertyPath.toString() to it.message }
            assertTrue(messages.contains("name" to "Name must be between 1 and 255 characters long"))
        }

    @Test
    fun `handle should accept valid name length`() =
        runTest {
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "A".repeat(255), // Maximum length
                    age = 30,
                    gender = "Female",
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should accept minimum valid name length`() =
        runTest {
            val request =
                MemberUpdateByIdHandler.Request(
                    name = "A", // Minimum length
                    age = 30,
                    gender = "Female",
                )
            val violations = validator.validate(request)
            assertTrue(violations.isEmpty())
        }

    @Test
    fun `handle should preserve email from existing member`() =
        runTest {
            TestUtils.mockUnitOfWork(unitOfWork, context)

            val memberId = UUID.randomUUID().toString()
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

            val response = handler.handle(memberId, request)

            assertEquals("preserved@example.com", response.email)
            verify(memberRepository).update(
                argThat { member ->
                    member.email == "preserved@example.com"
                },
            )
        }
}
