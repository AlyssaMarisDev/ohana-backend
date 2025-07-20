package com.ohana.api.task

import com.ohana.api.utils.getUserId
import com.ohana.domain.task.TaskCreationHandler
import com.ohana.domain.task.TaskDeleteByIdHandler
import com.ohana.domain.task.TaskGetAllHandler
import com.ohana.domain.task.TaskGetByIdHandler
import com.ohana.domain.task.TaskUpdateByIdHandler
import com.ohana.plugins.validateAndReceive
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class TaskController(
    private val taskCreationHandler: TaskCreationHandler,
    private val taskDeleteByIdHandler: TaskDeleteByIdHandler,
    private val taskGetAllHandler: TaskGetAllHandler,
    private val taskGetByIdHandler: TaskGetByIdHandler,
    private val taskUpdateByIdHandler: TaskUpdateByIdHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val householdId =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )

                    // Use annotation-based validation
                    val request = call.validateAndReceive<TaskCreationHandler.Request>()

                    val response = taskCreationHandler.handle(userId, householdId, request)

                    call.respond(HttpStatusCode.Created, response)
                }

                get("/{taskId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    val response = taskGetByIdHandler.handle(userId, id)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val householdIds =
                        call.request.queryParameters["householdIds"]
                            ?.split(",")
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList()

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = taskGetAllHandler.handle(userId, householdIds)

                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{taskId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    // Use annotation-based validation
                    val request = call.validateAndReceive<TaskUpdateByIdHandler.Request>()

                    val response = taskUpdateByIdHandler.handle(userId, id, request)

                    call.respond(HttpStatusCode.OK, response)
                }

                delete("/{taskId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    val deleted = taskDeleteByIdHandler.handle(userId, id)

                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}
