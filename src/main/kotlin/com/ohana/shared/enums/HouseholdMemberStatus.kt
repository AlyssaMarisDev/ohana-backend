package com.ohana.shared.enums

enum class HouseholdMemberStatus {
    /**
     * Member is active and can participate in household activities
     */
    ACTIVE,

    /**
     * Member has been invited but hasn't accepted yet
     */
    INVITED,

    /**
     * Member is inactive (e.g., left the household or was removed)
     */
    INACTIVE,
}
