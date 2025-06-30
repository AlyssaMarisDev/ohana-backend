package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.auth.handlers.MemberSignInHandler
import com.ohana.auth.handlers.RegisterNewMemberHandler
import com.ohana.health.controllers.HealthController
import com.ohana.members.controllers.MembersController
import com.ohana.members.handlers.GetAllMembersHandler
import com.ohana.members.handlers.GetSingleMemberByIdHandler
import com.ohana.members.handlers.UpdateMemberByIdHandler
import org.jdbi.v3.core.Jdbi
import org.koin.dsl.module

// Define a Koin module to provide dependencies
val appModule =
    module {
        // Provide a single instance of JDBI with environment-based configuration
        single {
            val dbHost = System.getenv("DB_HOST") ?: "localhost"
            val dbPort = System.getenv("DB_PORT") ?: "3306"
            val dbName = System.getenv("DB_NAME") ?: "ohana"
            val dbUser = System.getenv("DB_USER") ?: "root"
            val dbPassword = System.getenv("DB_PASSWORD") ?: "root"

            Jdbi.create("jdbc:mysql://$dbHost:$dbPort/$dbName", dbUser, dbPassword)
        }

        // Provide a single instance of services
        single { GetSingleMemberByIdHandler(get()) }
        single { GetAllMembersHandler(get()) }
        single { UpdateMemberByIdHandler(get()) }
        single { RegisterNewMemberHandler(get()) }
        single { MemberSignInHandler(get()) }

        // Provide a single instance of controllers
        single { MembersController(get(), get(), get()) }
        single { HealthController() }
        single { AuthController(get(), get()) }
    }
