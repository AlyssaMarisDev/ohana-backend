# API Design Patterns

## Overview

Ohana follows RESTful API design principles with consistent patterns for HTTP endpoints, request/response handling, and error management. All APIs are built using Ktor controllers and follow established conventions.

## Controller Pattern

### Structure

Controllers are responsible for:

1. **Route Registration** - Define HTTP endpoints
2. **Request Validation** - Validate incoming data
3. **Authentication/Authorization** - Check user permissions
4. **Handler Invocation** - Call business logic
5. **Response Formatting** - Return consistent responses

### Controller Template

```kotlin
class TaskController(
    private val taskCreationHandler: TaskCreationHandler,
    private val taskGetAllHandler: TaskGetAllHandler,
    private val taskUpdateHandler: TaskUpdateHandler,
) {
    fun Route.registerTaskRoutes() {
        authenticate("auth-jwt") {
            route("/tasks") {
                post("") {
                    val request = call.receive<TaskCreationHandler.Request>()

                    val validationErrors = request.validate()
                    if (validationErrors.isNotEmpty()) {
                        throw ValidationException("Validation failed: ${validationErrors.joinToString(", ")}")
                    }

                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val response = taskCreationHandler.handle(userId, request)

                    call.respond(HttpStatusCode.Created, response)
                }

                get("") {
                    val householdId = call.request.queryParameters["householdId"]
                        ?: throw ValidationException("householdId is required")

                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val response = taskGetAllHandler.handle(householdId, userId)

                    call.respond(HttpStatusCode.OK, response)
                }

                put("/{id}") {
                    val taskId = call.parameters["id"]
                        ?: throw ValidationException("Task ID is required")

                    val request = call.receive<TaskUpdateHandler.Request>()

                    val validationErrors = request.validate()
                    if (validationErrors.isNotEmpty()) {
                        throw ValidationException("Validation failed: ${validationErrors.joinToString(", ")}")
                    }

                    val userId = getUserId(call.principal<JWTPrincipal>())
                    val response = taskUpdateHandler.handle(taskId, userId, request)

                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}
```

## Authentication Pattern

### JWT Authentication

All protected endpoints use JWT authentication:

```kotlin
authenticate("auth-jwt") {
    route("/protected-endpoint") {
        // Protected routes here
    }
}
```

### User ID Extraction

Extract user ID from JWT token:

```kotlin
val userId = getUserId(call.principal<JWTPrincipal>())
```

### Public vs Protected Endpoints

```kotlin
fun Route.registerRoutes() {
    // Public endpoints (no authentication required)
    route("/auth") {
        post("/register") {
            // Registration endpoint
        }

        post("/login") {
            // Login endpoint
        }
    }

    // Protected endpoints (authentication required)
    authenticate("auth-jwt") {
        route("/tasks") {
            // Task endpoints require authentication
        }

        route("/households") {
            // Household endpoints require authentication
        }
    }
}
```

## Request/Response Pattern

### Request Validation

All requests follow a consistent validation pattern:

```kotlin
data class Request(
    val title: String,
    val description: String,
    val dueDate: Instant,
    val status: TaskStatus,
    val householdId: String,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (title.isEmpty()) errors.add("Title is required")
        if (title.length > 255) errors.add("Title must be at most 255 characters")
        if (description.isEmpty()) errors.add("Description is required")
        if (description.length > 1000) errors.add("Description must be at most 1000 characters")
        if (dueDate.isBefore(Instant.now())) errors.add("Due date cannot be in the past")
        if (!Guid.isValid(householdId)) errors.add("Household ID must be a valid GUID")

        return errors
    }
}
```

### Response Structure

All responses are consistent data classes:

```kotlin
data class Response(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Instant,
    val status: TaskStatus,
    val createdBy: String,
    val householdId: String,
)
```

### Validation in Controllers

```kotlin
post("") {
    val request = call.receive<Request>()

    val validationErrors = request.validate()
    if (validationErrors.isNotEmpty()) {
        throw ValidationException("Validation failed: ${validationErrors.joinToString(", ")}")
    }

    val response = handler.handle(request)
    call.respond(HttpStatusCode.Created, response)
}
```

## HTTP Status Codes

### Standard Status Codes

- **200 OK** - Successful GET/PUT operations
- **201 Created** - Successful POST operations
- **400 Bad Request** - Validation errors
- **401 Unauthorized** - Authentication failures
- **403 Forbidden** - Authorization failures
- **404 Not Found** - Resource not found
- **409 Conflict** - Duplicate/conflict errors
- **500 Internal Server Error** - Server errors

### Usage Examples

```kotlin
// Successful creation
call.respond(HttpStatusCode.Created, response)

// Successful retrieval
call.respond(HttpStatusCode.OK, response)

// Successful update
call.respond(HttpStatusCode.OK, response)

// No content (for deletions)
call.respond(HttpStatusCode.NoContent)
```

## Error Handling

### Exception Hierarchy

All custom exceptions implement `KnownError`:

```kotlin
interface KnownError {
    val message: String
}

class ValidationException(override val message: String) : Exception(message), KnownError
class AuthenticationException(override val message: String) : Exception(message), KnownError
class AuthorizationException(override val message: String) : Exception(message), KnownError
class NotFoundException(override val message: String) : Exception(message), KnownError
class ConflictException(override val message: String) : Exception(message), KnownError
class DbException(override val message: String) : Exception(message), KnownError
```

### Global Exception Handling

Exceptions are handled globally in `ExceptionHandling.kt`:

```kotlin
fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Validation failed"))
        }

        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Authentication failed"))
        }

        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Authorization failed"))
        }

        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Resource not found"))
        }

        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Conflict occurred"))
        }

        exception<DbException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Database error occurred"))
        }

        exception<Exception> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
}
```

### Error Response Format

```kotlin
data class ErrorResponse(
    val error: String,
    val timestamp: Instant = Instant.now(),
)
```

## Route Organization

### RESTful Endpoints

Follow REST conventions:

```kotlin
// Tasks
GET    /api/v1/tasks?householdId={id}     // Get all tasks for household
POST   /api/v1/tasks                      // Create new task
GET    /api/v1/tasks/{id}                 // Get specific task
PUT    /api/v1/tasks/{id}                 // Update task
DELETE /api/v1/tasks/{id}                 // Delete task

// Households
GET    /api/v1/households                 // Get user's households
POST   /api/v1/households                 // Create household
GET    /api/v1/households/{id}            // Get specific household
PUT    /api/v1/households/{id}            // Update household
DELETE /api/v1/households/{id}            // Delete household

// Members
GET    /api/v1/members                    // Get all members
GET    /api/v1/members/{id}               // Get specific member
PUT    /api/v1/members/{id}               // Update member profile
```

### Query Parameters

Use query parameters for filtering and pagination:

```kotlin
get("") {
    val householdId = call.request.queryParameters["householdId"]
        ?: throw ValidationException("householdId is required")

    val status = call.request.queryParameters["status"] // Optional filter
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

    val response = handler.handle(householdId, status, limit, offset)
    call.respond(HttpStatusCode.OK, response)
}
```

### Path Parameters

Use path parameters for resource identification:

```kotlin
put("/{id}") {
    val taskId = call.parameters["id"]
        ?: throw ValidationException("Task ID is required")

    if (!Guid.isValid(taskId)) {
        throw ValidationException("Task ID must be a valid GUID")
    }

    val request = call.receive<Request>()
    val response = handler.handle(taskId, request)
    call.respond(HttpStatusCode.OK, response)
}
```

## Content Negotiation

### JSON Serialization

All requests and responses use JSON:

```kotlin
// Request body
val request = call.receive<TaskCreationHandler.Request>()

// Response body
call.respond(HttpStatusCode.Created, response)
```

### Content Type Headers

Ktor automatically handles content type headers:

- `Content-Type: application/json` for requests
- `Content-Type: application/json` for responses

## CORS Configuration

CORS is configured globally for all origins:

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
}
```

## Rate Limiting

For future implementation, consider adding rate limiting:

```kotlin
// Example rate limiting configuration
install(RateLimit) {
    register {
        rateLimiter(limit = 100, refillPeriod = 1.minutes)
    }
}
```

## API Versioning

### URL Versioning

Use URL versioning for API changes:

```kotlin
route("/api/v1") {
    // Version 1 endpoints
}

route("/api/v2") {
    // Version 2 endpoints (future)
}
```

### Versioning Strategy

- **v1**: Current stable API
- **v2**: Future breaking changes
- Maintain backward compatibility within major versions

## Testing Controllers

### Controller Testing

```kotlin
@Test
fun `POST /tasks should create task when valid request`() = testApplication {
    // Setup
    val request = TaskCreationHandler.Request(
        title = "Test Task",
        description = "Test Description",
        dueDate = Instant.now().plusSeconds(3600),
        status = TaskStatus.pending,
        householdId = UUID.randomUUID().toString(),
    )

    // Execute
    client.post("/api/v1/tasks") {
        setBody(request)
        header(HttpHeaders.Authorization, "Bearer $validToken")
        contentType(ContentType.Application.Json)
    }.apply {
        // Assert
        assertEquals(HttpStatusCode.Created, status)
        val response = body<TaskCreationHandler.Response>()
        assertEquals(request.title, response.title)
    }
}
```

### Mocking Dependencies

```kotlin
@Test
fun `GET /tasks should return tasks for household`() = testApplication {
    // Mock handler
    val mockHandler = mock<TaskGetAllHandler>()
    whenever(mockHandler.handle(any(), any())).thenReturn(listOf(task))

    // Test implementation
}
```

## Best Practices

### 1. Consistent Naming

```kotlin
// ✅ Good - Consistent naming
class TaskController
class HouseholdController
class MemberController

// ❌ Bad - Inconsistent naming
class TaskController
class HouseholdsController
class UserController
```

### 2. Proper Error Handling

```kotlin
// ✅ Good - Proper error handling
val householdId = call.request.queryParameters["householdId"]
    ?: throw ValidationException("householdId is required")

// ❌ Bad - No error handling
val householdId = call.request.queryParameters["householdId"] ?: ""
```

### 3. Input Validation

```kotlin
// ✅ Good - Validate input
val taskId = call.parameters["id"]
    ?: throw ValidationException("Task ID is required")

if (!Guid.isValid(taskId)) {
    throw ValidationException("Task ID must be a valid GUID")
}

// ❌ Bad - No validation
val taskId = call.parameters["id"] ?: ""
```

### 4. Consistent Response Format

```kotlin
// ✅ Good - Consistent response
call.respond(HttpStatusCode.Created, response)

// ❌ Bad - Inconsistent response
call.respondText("Task created", ContentType.Text.Plain)
```

### 5. Authentication Check

```kotlin
// ✅ Good - Always check authentication
authenticate("auth-jwt") {
    route("/protected") {
        // Protected endpoints
    }
}

// ❌ Bad - No authentication
route("/protected") {
    // Unprotected endpoints
}
```

## Adding New Endpoints

### Steps to Add New Endpoint

1. **Create Handler** (if needed):

   ```kotlin
   class NewHandler(private val unitOfWork: UnitOfWork) {
       data class Request(val field: String) {
           fun validate(): List<String> = emptyList()
       }

       data class Response(val id: String, val field: String)

       suspend fun handle(request: Request): Response = // implementation
   }
   ```

2. **Add to Controller**:

   ```kotlin
   class NewController(private val handler: NewHandler) {
       fun Route.registerNewRoutes() {
           authenticate("auth-jwt") {
               route("/new-resource") {
                   post("") {
                       val request = call.receive<NewHandler.Request>()
                       val response = handler.handle(request)
                       call.respond(HttpStatusCode.Created, response)
                   }
               }
           }
       }
   }
   ```

3. **Register in Routing**:

   ```kotlin
   fun Application.configureRouting() {
       routing {
           newController.registerNewRoutes(this)
       }
   }
   ```

4. **Add to Dependency Injection**:
   ```kotlin
   single { NewController(get()) }
   single { NewHandler(get()) }
   ```

## Summary

The API design patterns provide:

- **Consistent** HTTP endpoint structure
- **RESTful** API design
- **Proper** authentication and authorization
- **Comprehensive** error handling
- **Testable** controller structure
- **Scalable** route organization

This approach ensures that all APIs follow the same patterns and are easy to understand, test, and maintain.
