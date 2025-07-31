package com.ohana.data.household

import com.ohana.shared.enums.HouseholdMemberRole
import com.ohana.shared.enums.HouseholdMemberStatus
import java.time.Instant

data class HouseholdMember(
    val id: String,
    val householdId: String,
    val memberId: String,
    val role: HouseholdMemberRole,
    val status: HouseholdMemberStatus,
    val isDefault: Boolean = false,
    val invitedBy: String? = null,
    val joinedAt: Instant? = null,
)
