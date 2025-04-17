package com.ohana.plugins

import com.ohana.controllers.MembersController
import com.ohana.data.MemberRepository
import com.ohana.handlers.GetSingleMemberService
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
