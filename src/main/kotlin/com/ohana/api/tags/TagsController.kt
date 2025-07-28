package com.ohana.api.tags

import com.ohana.api.tags.models.TagCreationRequest
import com.ohana.api.tags.models.TagGetAllRequest
import com.ohana.api.utils.getUserId
import com.ohana.domain.tags.TagCreationHandler
import com.ohana.domain.tags.TagGetAllHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class TagsController(
    private val tagGetAllHandler: TagGetAllHandler,
    private val tagCreationHandler: TagCreationHandler,
) {
    fun Route.registerTagsRoutes() {
        authenticate("auth-jwt") {
            route("/households") {
                route("/{householdId}/tags") {
                    get {
                        val userId = getUserId(call.principal<JWTPrincipal>())
                        val householdId =
                            call.parameters["householdId"]
                                ?: throw IllegalArgumentException("householdId parameter is required")

                        val request = TagGetAllRequest(householdId = householdId)

                        val response = tagGetAllHandler.handle(userId, request.toDomain())
                        call.respond(HttpStatusCode.OK, response)
                    }

                    post {
                        val userId = getUserId(call.principal<JWTPrincipal>())
                        val householdId =
                            call.parameters["householdId"]
                                ?: throw IllegalArgumentException("householdId parameter is required")

                        val request = call.receive<TagCreationRequest>()
                        val response = tagCreationHandler.handle(userId, householdId, request.toDomain())
                        call.respond(HttpStatusCode.Created, response)
                    }
                }
            }
        }
    }
}
