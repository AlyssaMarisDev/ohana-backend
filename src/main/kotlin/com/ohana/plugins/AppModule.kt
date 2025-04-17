package com.ohana.plugins

import com.ohana.members.controllers.MembersController
import com.ohana.members.handlers.GetSingleMemberByIdHandler
import com.ohana.auth.handlers.RegisterNewMemberHandler
import com.ohana.auth.handlers.MemberSignInHandler
import com.ohana.auth.controllers.AuthController
import com.ohana.health.controllers.HealthController
import org.jdbi.v3.core.Jdbi
import org.koin.dsl.module

// Define a Koin module to provide dependencies
val appModule = module {
    // Provide a single instance of JDBI
    single { Jdbi.create("jdbc:mysql://localhost:3306/ohana", "root", "root") }

    // Provide a single instance of services
    single { GetSingleMemberByIdHandler(get()) }
    single { RegisterNewMemberHandler(get()) }
    single { MemberSignInHandler(get()) }

    // Provide a single instance of controllers
    single { MembersController(get()) }
    single { HealthController() }
    single { AuthController(get(), get()) }
}
