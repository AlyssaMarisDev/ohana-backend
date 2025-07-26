package com.ohana.data.tags

import com.ohana.shared.enums.TagPermissionType
import java.time.Instant

data class TagPermission(
    val id: String,
    val householdMemberId: String,
    val permissionType: TagPermissionType,
    val tagIds: List<String>, // Tags the user can view
    val createdAt: Instant,
    val updatedAt: Instant,
)
