package com.ohana.data.permissions

import java.time.Instant

data class Permission(
    val id: String,
    val householdMemberId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
