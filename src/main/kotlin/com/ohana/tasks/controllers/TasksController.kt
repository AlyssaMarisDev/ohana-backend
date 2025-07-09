package com.ohana.tasks.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.tasks.handlers.TaskCreationHandler
import com.ohana.tasks.handlers.TaskGetByIdHandler
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class TasksController(
    private val taskCreationHandler: TaskCreationHandler,
    private val taskGetByIdHandler: TaskGetByIdHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val request = call.receive<TaskCreationHandler.Request>()
                    println("Received request: $request")

                    val userId =
                        call
                            .principal<JWTPrincipal>()
                            ?.payload
                            ?.getClaim("userId")
                            ?.asString()
                            ?: throw ValidationException("User ID is required")

                    val response = taskCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("/{id}") {
                    val id = call.parameters["id"] ?: throw ValidationException("Task ID is required")
                    val response = taskGetByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
