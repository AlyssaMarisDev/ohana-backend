package com.ohana.data.household

import com.ohana.shared.enums.HouseholdMemberRole

data class HouseholdMember(
    val id: String,
    val householdId: String,
    val memberId: String,
    val role: HouseholdMemberRole,
    val isActive: Boolean = false,
    val invitedBy: String? = null,
    val joinedAt: java.time.Instant? = null,
)
