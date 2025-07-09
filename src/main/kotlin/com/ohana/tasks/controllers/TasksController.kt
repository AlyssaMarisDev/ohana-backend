package com.ohana.tasks.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.tasks.handlers.TasksCreationHandler
import com.ohana.tasks.handlers.TasksGetAllHandler
import com.ohana.tasks.handlers.TasksGetByIdHandler
import com.ohana.utils.getUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class TasksController(
    private val tasksCreationHandler: TasksCreationHandler,
    private val tasksGetAllHandler: TasksGetAllHandler,
    private val tasksGetByIdHandler: TasksGetByIdHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val request = call.receive<TasksCreationHandler.Request>()
                    println("Received request: $request")

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = tasksCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("/{id}") {
                    val id = call.parameters["id"] ?: throw ValidationException("Task ID is required")
                    val response = tasksGetByIdHandler.handle(id)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = tasksGetAllHandler.handle()
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
