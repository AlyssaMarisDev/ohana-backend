package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.auth.handlers.MemberRegistrationHandler
import com.ohana.auth.handlers.MemberSignInHandler
import com.ohana.health.controllers.HealthController
import com.ohana.household.controllers.HouseholdController
import com.ohana.household.handlers.HouseholdAcceptInviteHandler
import com.ohana.household.handlers.HouseholdCreationHandler
import com.ohana.household.handlers.HouseholdGetAllHandler
import com.ohana.household.handlers.HouseholdGetByIdHandler
import com.ohana.household.handlers.HouseholdInviteMemberHandler
import com.ohana.members.controllers.MemberController
import com.ohana.members.handlers.MemberGetAllHandler
import com.ohana.members.handlers.MemberGetByIdHandler
import com.ohana.members.handlers.MemberUpdateByIdHandler
import com.ohana.tasks.controllers.TaskController
import com.ohana.tasks.handlers.TaskCreationHandler
import com.ohana.tasks.handlers.TaskGetAllHandler
import com.ohana.tasks.handlers.TaskGetByIdHandler
import com.ohana.tasks.handlers.TaskUpdateByIdHandler
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
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

            Jdbi
                .create("jdbc:mysql://$dbHost:$dbPort/$dbName", dbUser, dbPassword)
                .installPlugin(KotlinPlugin())
        }

        // Auth handlers
        single { MemberRegistrationHandler(get()) }
        single { MemberSignInHandler(get()) }

        // Members handlers
        single { MemberGetAllHandler(get()) }
        single { MemberGetByIdHandler(get()) }
        single { MemberUpdateByIdHandler(get()) }

        // Tasks handlers
        single { TaskCreationHandler(get()) }
        single { TaskGetAllHandler(get()) }
        single { TaskGetByIdHandler(get()) }
        single { TaskUpdateByIdHandler(get()) }

        // Household handlers
        single { HouseholdAcceptInviteHandler(get()) }
        single { HouseholdCreationHandler(get()) }
        single { HouseholdGetAllHandler(get()) }
        single { HouseholdGetByIdHandler(get()) }
        single { HouseholdInviteMemberHandler(get()) }
        // Controllers
        single { AuthController(get(), get()) }
        single { HealthController() }
        single { HouseholdController(get(), get(), get(), get(), get()) }
        single { MemberController(get(), get(), get()) }
        single { TaskController(get(), get(), get(), get()) }
    }
