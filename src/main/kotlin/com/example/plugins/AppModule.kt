package com.example.plugins

import com.example.controllers.MembersController
import com.example.data.MemberRepository
import com.example.services.GetSingleMemberService
import org.koin.dsl.module

// Define a Koin module to provide dependencies
val appModule = module {
    // Provide a single instance of repositories
    single { MemberRepository() }

    // Provide a single instance of services
    single { GetSingleMemberService(get()) }

    // Provide a single instance of controllers
    single { MembersController(get()) }
}
