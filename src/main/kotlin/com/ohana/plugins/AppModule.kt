package com.ohana.plugins

import com.ohana.api.auth.AuthController
import com.ohana.api.health.HealthController
import com.ohana.api.household.HouseholdController
import com.ohana.api.member.MemberController
import com.ohana.api.tags.TagsController
import com.ohana.api.task.TaskController
import com.ohana.data.unitOfWork.JdbiUnitOfWork
import com.ohana.data.unitOfWork.UnitOfWork
import com.ohana.domain.auth.*
import com.ohana.domain.household.*
import com.ohana.domain.member.*
import com.ohana.domain.permissions.TagPermissionManager
import com.ohana.domain.tags.DefaultTagService
import com.ohana.domain.tags.TagCreationHandler
import com.ohana.domain.tags.TagDeleteHandler
import com.ohana.domain.tags.TagGetAllHandler
import com.ohana.domain.tags.TagUpdateHandler
import com.ohana.domain.tags.TaskTagManager
import com.ohana.domain.task.*
import com.ohana.domain.validators.HouseholdMemberValidator
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
        single { TaskTagManager() }
        single { TagPermissionManager(get()) }
        single { DefaultTagService() }

        // Auth handlers
        single { RegistrationHandler(get()) }
        single { LoginHandler(get()) }
        single { TokenRefreshHandler(get()) }
        single { LogoutHandler(get()) }

        // Members handlers - now only need UnitOfWork
        single { MemberGetAllHandler(get(), get()) }
        single { MemberGetByIdHandler(get()) }
        single { MemberUpdateByIdHandler(get()) }

        // Tasks handlers - now only need UnitOfWork
        single { TaskCreationHandler(get(), get(), get()) }
        single { TaskDeleteByIdHandler(get(), get()) }
        single { TaskGetAllHandler(get(), get(), get(), get()) }
        single { TaskGetByIdHandler(get(), get(), get()) }
        single { TaskUpdateByIdHandler(get(), get(), get()) }

        // Household handlers
        single { HouseholdAcceptInviteHandler(get()) }
        single { HouseholdCreationHandler(get(), get(), get()) }
        single { HouseholdGetAllHandler(get()) }
        single { HouseholdGetByIdHandler(get(), get()) }
        single { HouseholdInviteMemberHandler(get()) }

        // Tags handlers
        single { TagGetAllHandler(get(), get(), get()) }
        single { TagCreationHandler(get(), get()) }
        single { TagUpdateHandler(get(), get()) }
        single { TagDeleteHandler(get(), get()) }

        // Controllers
        single { AuthController(get(), get(), get(), get()) }
        single { HealthController() }
        single { HouseholdController(get(), get(), get(), get(), get()) }
        single { MemberController(get(), get(), get()) }
        single { TaskController(get(), get(), get(), get(), get()) }
        single { TagsController(get(), get(), get(), get()) }
    }
