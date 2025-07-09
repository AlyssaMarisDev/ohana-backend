package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.auth.handlers.MemberRegistrationHandler
import com.ohana.auth.handlers.MemberSignInHandler
import com.ohana.health.controllers.HealthController
import com.ohana.members.controllers.MembersController
import com.ohana.members.handlers.MembersGetAllHandler
import com.ohana.members.handlers.MembersGetByIdHandler
import com.ohana.members.handlers.MembersUpdateByIdHandler
import com.ohana.tasks.controllers.TasksController
import com.ohana.tasks.handlers.TaskCreationHandler
import com.ohana.tasks.handlers.TaskGetByIdHandler
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
        single { MembersGetAllHandler(get()) }
        single { MembersGetByIdHandler(get()) }
        single { MembersUpdateByIdHandler(get()) }

        // Tasks handlers
        single { TaskCreationHandler(get()) }
        single { TaskGetByIdHandler(get()) }

        // Controllers
        single { AuthController(get(), get()) }
        single { HealthController() }
        single { MembersController(get(), get(), get()) }
        single { TasksController(get(), get()) }
    }
