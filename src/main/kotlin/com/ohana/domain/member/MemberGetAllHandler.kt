package com.ohana.domain.member

import com.ohana.shared.UnitOfWork

class MemberGetAllHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Response(
        val id: String,
        val name: String,
        val age: Int?,
        val gender: String?,
        val email: String,
    )

    suspend fun handle(): List<Response> =
        unitOfWork.execute { context ->
            context.members.findAll().map { member ->
                Response(
                    id = member.id,
                    name = member.name,
                    age = member.age,
                    gender = member.gender,
                    email = member.email,
                )
            }
        }
}
