package com.ohana.plugins

import io.ktor.server.application.*
import org.flywaydb.core.Flyway


fun Application.configureDatabase() {
    Flyway.configure()
        .dataSource(
            "jdbc:mysql://localhost:3306/OhanaMembers?useSSL=false&serverTimezone=UTC",
            "root",
            "root"
        )
        .load()
        .migrate()
}
