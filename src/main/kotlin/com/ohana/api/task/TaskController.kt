package com.ohana.api.task

import com.ohana.api.task.models.TaskCreationRequest
import com.ohana.api.task.models.TaskGetAllRequest
import com.ohana.api.task.models.TaskUpdateRequest
import com.ohana.api.utils.getUserId
import com.ohana.domain.task.TaskCreationHandler
import com.ohana.domain.task.TaskDeleteByIdHandler
import com.ohana.domain.task.TaskGetAllHandler
import com.ohana.domain.task.TaskGetByIdHandler
import com.ohana.domain.task.TaskUpdateByIdHandler
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

                    val request = call.receive<TaskCreationRequest>()
                    val domainRequest = request.toDomain()

                    val response = taskCreationHandler.handle(userId, domainRequest)

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
                    val request = TaskGetAllRequest(householdIds)
                    val domainRequest = request.toDomain()

                    val response = taskGetAllHandler.handle(userId, domainRequest)

                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{taskId}") {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val id =
                        call.parameters["taskId"]
                            ?: throw ValidationException("Task ID is required", listOf(ValidationError("taskId", "Task ID is required")))

                    val request = call.receive<TaskUpdateRequest>()
                    val domainRequest = request.toDomain()
                    val response = taskUpdateByIdHandler.handle(userId, id, domainRequest)

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
