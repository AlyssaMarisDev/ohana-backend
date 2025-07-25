package com.ohana.data.household

import java.time.Instant

data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val householdId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
