package com.ohana.data.household

interface HouseholdRepository {
    fun findById(id: String): Household?

    fun findAll(): List<Household>

    fun findByMemberId(memberId: String): List<Household>

    fun create(household: Household): Household

    fun findMemberById(
        householdId: String,
        memberId: String,
    ): HouseholdMember?

    fun findMembersByHouseholdId(householdId: String): List<HouseholdMember>

    fun createMember(member: HouseholdMember): HouseholdMember

    fun updateMember(member: HouseholdMember): HouseholdMember

    fun findDefaultHouseholdByMemberId(memberId: String): HouseholdMember?

    fun setDefaultHousehold(
        memberId: String,
        householdId: String,
    ): HouseholdMember
}
