package com.example.models

import kotlinx.serialization.Serializable

@Serializable
class Member (
    val id: Int,
    val name: String,
    val age: Int,
    val gender: String,
    val email: String,
    val password: String,
)