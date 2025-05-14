package com.ohana

import com.ohana.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin

fun main() {
    // Read the port from the environment variable, default to 4242 if not set
    val port = System.getenv("PORT")?.toIntOrNull() ?: 4242

    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Koin) {
        modules(appModule)
    }

    configureSecurity()
    configureSerialization()
    configureRouting()
    configureCallLogging()
    configureExceptionHandling()
}
