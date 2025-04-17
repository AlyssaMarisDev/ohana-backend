package com.ohana.plugins

import com.ohana.members.controllers.MembersController
import com.ohana.members.handlers.GetSingleMemberByIdHandler
import org.jdbi.v3.core.Jdbi
import org.koin.dsl.module

// Define a Koin module to provide dependencies
val appModule = module {
    // Provide a single instance of services
    single { GetSingleMemberByIdHandler(get()) }

    // Provide a single instance of controllers
    single { MembersController(get()) }

    // Provide a single instance of JDBI
    single { Jdbi.create("jdbc:mysql://localhost:3306/ohana", "root", "root") }
}
