package com.ohana.member.entities

data class Member(
    val id: String,
    val name: String,
    val email: String,
    val age: Int? = null,
    val gender: String? = null,
)
