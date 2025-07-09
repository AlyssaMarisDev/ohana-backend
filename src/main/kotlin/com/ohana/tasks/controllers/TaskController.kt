package com.ohana.tasks.controllers

import com.ohana.exceptions.ValidationException
import com.ohana.tasks.handlers.TaskCreationHandler
import com.ohana.tasks.handlers.TaskGetAllHandler
import com.ohana.tasks.handlers.TaskGetByIdHandler
import com.ohana.tasks.handlers.TaskUpdateByIdHandler
import com.ohana.utils.getUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class TaskController(
    private val taskCreationHandler: TaskCreationHandler,
    private val taskGetAllHandler: TaskGetAllHandler,
    private val taskGetByIdHandler: TaskGetByIdHandler,
    private val taskUpdateByIdHandler: TaskUpdateByIdHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val request = call.receive<TaskCreationHandler.Request>()
                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = taskCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("/{id}") {
                    val id = call.parameters["id"] ?: throw ValidationException("Task ID is required")

                    val response = taskGetByIdHandler.handle(id)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val response = taskGetAllHandler.handle()

                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{id}") {
                    val id = call.parameters["id"] ?: throw ValidationException("Task ID is required")
                    val request = call.receive<TaskUpdateByIdHandler.Request>()

                    val response = taskUpdateByIdHandler.handle(id, request)

                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
