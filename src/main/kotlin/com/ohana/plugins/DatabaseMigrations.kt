package com.ohana.plugins

import org.flywaydb.core.Flyway

fun runDatabaseMigrations(config: DatabaseConfig) {
    val jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.name}"
    val flyway =
        Flyway
            .configure()
            .dataSource(jdbcUrl, config.user, config.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()

    flyway.migrate()
}
