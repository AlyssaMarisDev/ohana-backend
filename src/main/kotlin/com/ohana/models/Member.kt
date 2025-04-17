package com.ohana.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

object Members : IntIdTable() {
    val name = varchar("name", 255)
    val age = integer("age")
    val gender = varchar("gender", 255)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
}

// Define the Member entity class
class MemberEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MemberEntity>(Members)

    var name by Members.name
    var age by Members.age
    var gender by Members.gender
    var email by Members.email
    var password by Members.password

    // Convert the MemberEntity to a regular Member data class
    fun toMember() = Member(
        id = this.id.value,
        name = this.name,
        age = this.age,
        gender = this.gender,
        email = this.email,
        password = this.password
    )
}

data class Member (
    val id: Int,
    val name: String,
    val age: Int,
    val gender: String,
    val email: String,
    val password: String,
)