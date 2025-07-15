package com.ohana.member.handlers

import com.ohana.exceptions.NotFoundException
import com.ohana.exceptions.ValidationError
import com.ohana.shared.UnitOfWork
import com.ohana.shared.Validatable

class MemberUpdateByIdHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(
        val name: String,
        val age: Int?,
        val gender: String?,
    ) : Validatable {
        override fun validate(): List<ValidationError> {
            val errors = mutableListOf<ValidationError>()

            if (name.isEmpty()) errors.add(ValidationError("name", "Name is required"))
            if (name.length < 1) errors.add(ValidationError("name", "Name must be at least 1 character long"))
            if (name.length > 255) errors.add(ValidationError("name", "Name must be at most 255 characters long"))

            return errors
        }
    }

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
