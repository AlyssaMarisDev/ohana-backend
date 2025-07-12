# Ohana Backend Architecture Patterns

## Overview

This is a Kotlin-based REST API built with Ktor, using a layered architecture with clear separation of concerns. The application follows specific patterns and conventions that must be maintained when making changes.

## Core Architecture Principles

### 1. Layered Architecture

The application follows a strict layered architecture:

- **Controllers** - Handle HTTP requests/responses and route definitions
- **Handlers** - Contain business logic and orchestrate operations
- **Database Utils** - Provide database access abstractions
- **Exceptions** - Custom exception hierarchy for error handling

### 2. Dependency Injection Pattern

- Uses **Koin** for dependency injection
- All dependencies are defined in `AppModule.kt`
- Controllers and Handlers are injected as singletons
- Database connection (JDBI) is injected as a single instance

### 3. Request-Response Pattern

Every handler follows this pattern:

```kotlin
data class Request(
    // Input parameters
)

data class Response(
    // Output data
)

suspend fun handle(request: Request, *other params if applicable*): Response {
    // Business logic
}
```

## Package Structure and Conventions

### Package Organization

```
com.ohana/
├── Application.kt                   # Main entry point
├── auth/                            # Authentication domain
│   ├── controllers/                 # HTTP controllers
│   ├── handlers/                    # Business logic
│   ├── services/                    # CRUD services
│   ├── repositories/                # Database repositories
│   └── utils/                       # Auth utilities (JWT, hashing)
├── exceptions/                      # Custom exception hierarchy
├── health/                          # Health check endpoints
├── household/                       # Household domain
│   ├── controllers/
│   ├── handlers/
│   ├── services/
│   └── repositories/
├── member/                          # Member domain
│   ├── controllers/
│   ├── handlers/
│   ├── services/
│   └── repositories/
├── plugins/                         # Ktor application configuration
├── shared/                          # Shared enums and constants
├── task/                            # Task domain
│   ├── controllers/
│   ├── handlers/
│   ├── services/
│   └── repositories/
└── utils/                           # Shared utilities
```

### Naming Conventions

- **Controllers**: `{Domain}Controller.kt` (e.g., `AuthController.kt`)
- **Handlers**: `{Domain}{Action}Handler.kt` (e.g., `MemberRegistrationHandler.kt`)
- **Services**: `{Resource}Service.kt` (e.g., `MemberService.kt`)
- **Repositories**: `{Entity}Repository.kt` (e.g., `MemberRepository.kt`)
- **Request/Response**: Nested data classes within handlers
- **Database tables**: Plural, snake_case (e.g., `household_members`)

## Controller Pattern

### Structure

Controllers are responsible for:

1. Route registration
2. Request validation
3. Authentication/authorization
4. Calling handlers
5. Response formatting

### Template

```kotlin
class {Domain}Controller(
    private val {domain}{Action}Handler: {Domain}{Action}Handler,
    // ... other handlers
) {
    fun Route.register{Domain}Routes() {
        authenticate("auth-jwt") {  // If authentication required
            route("/{resource}") {
                // HTTP method handlers
            }
        }
    }
}
```

### Authentication Pattern

- Public endpoints: No `authenticate` block
- Protected endpoints: Wrap in `authenticate("auth-jwt")`
- Extract user ID: `getUserId(call.principal<JWTPrincipal>())`

## Handler Pattern

### Structure

Handlers contain business logic and follow this pattern:

```kotlin
class {Action}Handler(
    private val {Resource1}Service,
    private val {Resource2}Service,
) {
    data class Request(
        // Input validation
        fun validate(): List<String> {
            // Return list of validation errors
        }
    )

    data class Response(
        // Output data
    )

    suspend fun handle(request: Request, *other params if applicable*): Response {
        // Call services to create/read/update/delete data
        // Does business logic using those resources
    }
}
```

### Database Operations

Always use `{Resource}Service.startTransaction` to start a transaction

- `startTransaction` returns an id for the transaction
- `useTransaction` can accept an id to continue the transaction using another Service
- `commitTransaction` on any Service will commit that transaction for all Services using it

Example:

```kotlin
val transactionId = memberService.startTransaction()
// Udate a member

```

## Exception Handling Pattern

### Custom Exception Hierarchy

All custom exceptions implement `KnownError` interface:

- `ValidationException` - Input validation errors (400)
- `AuthenticationException` - Auth failures (401)
- `AuthorizationException` - Permission errors (403)
- `NotFoundException` - Resource not found (404)
- `ConflictException` - Duplicate/conflict errors (409)
- `DbException` - Database errors (500)

### Exception Usage

- Throw specific exceptions in handlers
- Global exception handling in `ExceptionHandling.kt`
- Use `KnownError` interface to distinguish custom exceptions

## Database Pattern

### Connection Management

- Single JDBI instance injected via Koin
- Environment-based configuration (DB_HOST, DB_PORT, etc.)
- Automatic connection pooling via JDBI

### Query Patterns

```kotlin
// Parameterized queries with named parameters
val query = "SELECT * FROM table WHERE id = :id"
val params = mapOf("id" to id)

// Use DatabaseUtils for all operations
get(handle, query, params, Response::class)
```

### Transaction Management

```kotlin
transaction(jdbi) { handle ->
    // Multiple database operations
    // Automatic rollback on exception
}
```

## Validation Pattern

### Request Validation

- Implement `validate()` method in Request data classes
- Return `List<String>` of error messages
- Check in controllers before calling handlers

### Example

```kotlin
data class Request(
    val email: String,
    val password: String,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (email.isEmpty()) errors.add("Email is required")
        if (!email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"))) {
            errors.add("Invalid email format")
        }
        return errors
    }
}
```

## Authentication & Authorization

### JWT Authentication

- JWT tokens with HMAC256 algorithm
- User ID stored in JWT payload as "userId"
- Environment variable: `JWT_SECRET`

### Authorization Pattern

```kotlin
// Check if user is member of household
val member = getHouseholdMember(handle, userId, householdId)
if (member == null) {
    throw AuthorizationException("User is not a member of the household")
}

// Check role-based permissions
if (member.role != HouseholdMemberRole.admin.name) {
    throw AuthorizationException("User is not an admin of the household")
}
```

## API Design Patterns

### RESTful Endpoints

- Base path: `/api/v1`
- Resource-based URLs: `/members`, `/households`, `/tasks`
- HTTP methods: GET, POST, PUT, DELETE
- Consistent response formats

### Response Status Codes

- 200 OK - Successful GET/PUT operations
- 201 Created - Successful POST operations
- 400 Bad Request - Validation errors
- 401 Unauthorized - Authentication failures
- 403 Forbidden - Authorization failures
- 404 Not Found - Resource not found
- 409 Conflict - Duplicate/conflict errors
- 500 Internal Server Error - Server errors

## Configuration Pattern

### Environment Variables

- `PORT` - Server port (default: 4242)
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 3306)
- `DB_NAME` - Database name (default: ohana)
- `DB_USER` - Database user (default: root)
- `DB_PASSWORD` - Database password (default: root)
- `JWT_SECRET` - JWT signing secret

### Application Configuration

- Ktor plugins configured in separate files under `plugins/`
- CORS enabled for all origins
- Jackson serialization with Java 8 time support

## Testing Patterns

### Test Structure

- JUnit 5 for unit testing
- H2 in-memory database for tests
- Ktor test host for integration testing
- Coroutines test utilities

### Test Dependencies

- Inject test doubles via Koin test modules
- Use `runTest` for coroutine testing
- Mock external dependencies

## Logging Pattern

### Logging Configuration

- Logback for logging framework
- Console appender with structured format
- Debug level for development
- INFO level for production

### Logging Usage

```kotlin
private val logger = LoggerFactory.getLogger(this::class.java)
logger.info("Operation completed")
logger.error("Error occurred", exception)
```

## Security Patterns

### Password Security

- Salted password hashing
- Strong password requirements
- Secure salt generation

### Input Validation

- Validate all user inputs
- Use parameterized queries (SQL injection prevention)
- Sanitize data before database operations

## Performance Patterns

### Database Optimization

- Use indexes on frequently queried columns
- Implement pagination for large datasets
- Use transactions for multi-step operations
- Connection pooling via JDBI

### Async Operations

- Use coroutines for I/O operations
- Suspend functions for database operations
- Non-blocking HTTP handling via Ktor

## Error Handling Guidelines

### When Adding New Features

1. Create appropriate custom exceptions
2. Add exception handlers in `ExceptionHandling.kt`
3. Use proper HTTP status codes
4. Provide meaningful error messages
5. Log errors appropriately

### Database Error Handling

- Always use `DatabaseUtils` methods
- Handle `SQLIntegrityConstraintViolationException` as `ConflictException`
- Wrap unknown database errors in `DbException`
- Use transactions for multi-step operations

## Code Quality Standards

### Kotlin Conventions

- Use data classes for Request/Response objects
- Prefer immutable objects
- Use extension functions for utilities
- Follow Kotlin naming conventions

### Code Organization

- Single responsibility principle
- Dependency injection for all dependencies
- Clear separation between layers
- Consistent error handling

## Adding New Features

### Steps to Follow

1. **Add shared enums/constants** in `shared/` package
2. **Create handler** with Request/Response data classes
3. **Create controller** with route registration
4. **Add to Koin module** in `AppModule.kt`
5. **Add to routing** in `Routing.kt`
6. **Add tests** for new functionality

### Example: Adding a New Resource

```kotlin
// 1. Create handler
class NewResourceHandler(private val jdbi: Jdbi) {
    data class Request(val name: String)
    data class Response(val id: String, val name: String)

    suspend fun handle(request: Request): Response {
        return transaction(jdbi) { handle ->
            // Implementation
        }
    }
}

// 2. Create controller
class NewResourceController(private val handler: NewResourceHandler) {
    fun Route.registerNewResourceRoutes() {
        authenticate("auth-jwt") {
            route("/new-resources") {
                post("") {
                    val request = call.receive<NewResourceHandler.Request>()
                    val response = handler.handle(request)
                    call.respond(HttpStatusCode.Created, response)
                }
            }
        }
    }
}

// 3. Add to AppModule.kt
single { NewResourceHandler(get()) }
single { NewResourceController(get()) }

// 4. Add to Routing.kt
val newResourceController: NewResourceController by inject()
newResourceController.apply {
    registerNewResourceRoutes()
}
```

This architecture ensures maintainability, testability, and consistency across the application. Always follow these patterns when making changes to maintain code quality and consistency.
