package com.ohana.domain.member

import com.ohana.exceptions.NotFoundException
import com.ohana.shared.UnitOfWork
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class MemberUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        @field:NotBlank(message = "Name is required")
        @field:Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters long")
        val name: String,
        val age: Int?,
        val gender: String?,
    )

    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(
        id: String,
        request: Request,
    ): Response =
        unitOfWork.execute { context ->
            // Get existing member
            val existingMember = context.members.findById(id) ?: throw NotFoundException("Member not found")

            // Update member
            val updatedMember =
                context.members.update(
                    existingMember.copy(
                        name = request.name,
                        age = request.age,
                        gender = request.gender,
                    ),
                )

            // Return response
            Response(
                id = updatedMember.id,
                name = updatedMember.name,
                age = updatedMember.age,
                gender = updatedMember.gender,
                email = updatedMember.email,
            )
        }
}
