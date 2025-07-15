package com.ohana.plugins

import com.ohana.auth.controllers.AuthController
import com.ohana.auth.handlers.MemberRegistrationHandler
import com.ohana.auth.handlers.MemberSignInHandler
import com.ohana.config.AppConfig
import com.ohana.health.controllers.HealthController
import com.ohana.household.controllers.HouseholdController
import com.ohana.household.handlers.*
import com.ohana.member.controllers.MemberController
import com.ohana.member.handlers.*
import com.ohana.shared.HouseholdMemberValidator
import com.ohana.shared.JdbiUnitOfWork
import com.ohana.shared.UnitOfWork
import com.ohana.task.controllers.TaskController
import com.ohana.task.handlers.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.koin.dsl.module

// Define a Koin module to provide dependencies
val appModule =
    module {
        // Provide configuration
        single { AppConfig.fromEnvironment() }

        // Provide a single instance of JDBI with environment-based configuration
        single {
            val config = AppConfig.fromEnvironment()
            Jdbi
                .create(
                    "jdbc:mysql://${config.database.host}:${config.database.port}/${config.database.name}",
                    config.database.user,
                    config.database.password,
                ).installPlugin(KotlinPlugin())
        }

        // Unit of Work - provides services through context
        single<UnitOfWork> { JdbiUnitOfWork(get()) }

        // Shared
        single { HouseholdMemberValidator() }

        // Auth handlers
        single { MemberRegistrationHandler(get()) }
        single { MemberSignInHandler(get()) }

        // Members handlers - now only need UnitOfWork
        single { MemberGetAllHandler(get()) }
        single { MemberGetByIdHandler(get()) }
        single { MemberUpdateByIdHandler(get()) }

        // Tasks handlers - now only need UnitOfWork
        single { TaskCreationHandler(get(), get()) }
        single { TaskGetAllHandler(get(), get()) }
        single { TaskGetByIdHandler(get(), get()) }
        single { TaskUpdateByIdHandler(get(), get()) }

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
