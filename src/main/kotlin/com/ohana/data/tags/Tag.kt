package com.ohana.data.tags

import java.time.Instant

data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val householdId: String?,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
