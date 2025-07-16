package com.ohana.task.controllers

import com.ohana.exceptions.ValidationError
import com.ohana.exceptions.ValidationException
import com.ohana.plugins.validateAndReceive
import com.ohana.task.handlers.*
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
                    val householdId =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    val response = taskGetByIdHandler.handle(id, householdId, userId)

                    call.respond(HttpStatusCode.OK, response)
                }

                get("") {
                    val householdIdsParam =
                        call.request.queryParameters["householdIds"]
                            ?: throw ValidationException(
                                "Household IDs are required",
                                listOf(ValidationError("householdIds", "Household IDs are required")),
                            )

                    val householdIds = householdIdsParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (householdIds.isEmpty()) {
                        throw ValidationException(
                            "At least one household ID is required",
                            listOf(ValidationError("householdIds", "At least one household ID is required")),
                        )
                    }

                    val userId = getUserId(call.principal<JWTPrincipal>())

                    val response = taskGetAllHandler.handle(householdIds, userId)

                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{taskId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val householdId =
                        call.parameters["householdId"]
                            ?: throw ValidationException(
                                "Household ID is required",
                                listOf(ValidationError("householdId", "Household ID is required")),
                            )
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    // Use annotation-based validation
                    val request = call.validateAndReceive<TaskUpdateByIdHandler.Request>()

                    val response = taskUpdateByIdHandler.handle(id, householdId, userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
