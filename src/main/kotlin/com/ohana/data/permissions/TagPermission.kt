package com.ohana.data.permissions

import java.time.Instant

data class TagPermission(
    val id: String,
    val permissionId: String,
    val tagId: String,
    val createdAt: Instant,
)
