package com.ohana.api.tags

import com.ohana.api.utils.getUserId
import com.ohana.domain.tags.GetTagsHandler
import com.ohana.shared.exceptions.ValidationError
import com.ohana.shared.exceptions.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

class TagsController(
    private val getTagsHandler: GetTagsHandler,
) {
    fun Route.registerTagsRoutes() {
        authenticate("auth-jwt") {
            route("/tags") {
                get {
                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val householdId =
                        call.request.queryParameters["householdId"]
                            ?: throw ValidationException(
                                "householdId is required",
                                listOf(ValidationError("householdId", "householdId is required")),
                            )

                    val response = getTagsHandler.handle(userId, householdId)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
