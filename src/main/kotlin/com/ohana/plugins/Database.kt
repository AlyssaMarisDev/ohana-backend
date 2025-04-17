package com.ohana.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    Database.connect(
        url = "jdbc:mysql://localhost:3306/OhanaMembers?useSSL=false&serverTimezone=UTC",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "development"
    )
}
