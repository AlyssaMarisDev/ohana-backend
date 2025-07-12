# Ohana Backend Architecture Patterns

## Overview

This is a Kotlin-based REST API built with Ktor, using a layered architecture with clear separation of concerns. The application follows specific patterns and conventions that must be maintained when making changes.

## Core Architecture Principles

### 1. Layered Architecture

The application follows a strict layered architecture:

- **Controllers** - Handle HTTP requests/responses and route definitions
- **Handlers** - Contain business logic and orchestrate operations
- **Repositories** - Provide data access abstractions through the Unit of Work pattern
- **Unit of Work** - Manages transactions and provides repository access
- **Exceptions** - Custom exception hierarchy for error handling

### 2. Dependency Injection Pattern

- Uses **Koin** for dependency injection
- All dependencies are defined in `AppModule.kt`
- Controllers and Handlers are injected as singletons
- Unit of Work is injected as a single instance
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
│   ├── entities/                    # Domain entities (AuthMember)
│   ├── repositories/                # Database repositories
│   └── utils/                       # Auth utilities (JWT, hashing)
├── exceptions/                      # Custom exception hierarchy
├── health/                          # Health check endpoints
├── household/                       # Household domain
│   ├── controllers/
│   ├── handlers/
│   ├── repositories/
│   └── entities/
├── member/                          # Member domain
│   ├── controllers/
│   ├── handlers/
│   ├── entities/
│   └── repositories/
├── plugins/                         # Ktor application configuration
├── shared/                          # Shared enums, constants, and Unit of Work
├── task/                            # Task domain
│   ├── controllers/
│   ├── handlers/
│   ├── entities/
│   └── repositories/
└── utils/                           # Shared utilities
```

### Naming Conventions

- **Controllers**: `{Domain}Controller.kt` (e.g., `AuthController.kt`)
- **Handlers**: `{Domain}{Action}Handler.kt` (e.g., `MemberRegistrationHandler.kt`)
- **Repositories**: `Jdbi{Entity}Repository.kt` (e.g., `JdbiMemberRepository.kt`)
- **Entities**: `{Entity}.kt` (e.g., `Member.kt`, `Task.kt`)
- **Request/Response**: Nested data classes within handlers
- **Database tables**: Plural, snake_case (e.g., `household_members`)

## Unit of Work Pattern

### Overview

The Unit of Work pattern provides a centralized way to manage database transactions and repository access. All database operations go through the Unit of Work, ensuring consistent transaction management.

### Structure

```kotlin
// Unit of Work interface
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

// Unit of Work context interface
interface UnitOfWorkContext {
    val tasks: TaskRepository
    val members: MemberRepository
    val households: HouseholdRepository
    val authMembers: AuthMemberRepository
}

// Repository interfaces
interface TaskRepository {
    fun create(task: Task): Task
    fun findById(id: String): Task?
    fun findAll(): List<Task>
    fun update(task: Task): Task
}

interface MemberRepository {
    fun findById(id: String): Member?
    fun findAll(): List<Member>
    fun findByEmail(email: String): Member?
    fun create(member: Member): Member
    fun update(member: Member): Member
}

interface HouseholdRepository {
    fun findById(id: String): Household?
    fun findAll(): List<Household>
    fun create(household: Household): Household
    fun findMemberById(householdId: String, memberId: String): HouseholdMember?
    fun createMember(member: HouseholdMember): HouseholdMember
    fun updateMember(member: HouseholdMember): HouseholdMember
}

interface AuthMemberRepository {
    fun findByEmail(email: String): AuthMember?
    fun create(member: AuthMember): AuthMember
}
```

### Usage in Handlers

```kotlin
class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    suspend fun handle(userId: String, request: Request): Response =
        unitOfWork.execute { context ->
            // Validate that the user exists
            context.members.findById(userId)
                ?: throw IllegalArgumentException("User not found")

            // Create the task
            val task = context.tasks.create(
                Task(
                    id = request.id,
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                    createdBy = userId,
                )
            )

            // Convert to response
            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                createdBy = task.createdBy,
            )
        }
}
```

### Benefits

- **Automatic transaction management**: All operations within `unitOfWork.execute` are wrapped in a transaction
- **Consistent error handling**: Database errors are automatically converted to appropriate exceptions
- **Repository abstraction**: Handlers don't need to know about database implementation details
- **Testability**: Easy to mock the Unit of Work for testing
- **Type safety**: Strongly typed repository interfaces

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
    private val unitOfWork: UnitOfWork,
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
        return unitOfWork.execute { context ->
            // Business logic using repositories
            // All database operations go through context.{repository}
        }
    }
}
```

### Database Operations

Always use the Unit of Work pattern for database operations:

```kotlin
suspend fun handle(request: Request): Response =
    unitOfWork.execute { context ->
        // Get data
        val member = context.members.findById(id)
            ?: throw NotFoundException("Member not found")

        // Create data
        val task = context.tasks.create(newTask)

        // Update data
        val updatedMember = context.members.update(member.copy(name = "New Name"))

        // Return response
        Response(...)
    }
```

## Repository Pattern

### Implementation

Each repository implements a specific interface and provides data access methods:

```kotlin
class JdbiTaskRepository(
    private val handle: Handle,
) : TaskRepository {
    override fun create(task: Task): Task {
        val insertQuery = """
            INSERT INTO tasks (id, title, description, due_date, status, created_by)
            VALUES (:id, :title, :description, :due_date, :status, :created_by)
        """

        val insertedRows = DatabaseUtils.insert(
            handle,
            insertQuery,
            mapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "due_date" to task.dueDate,
                "status" to task.status.name,
                "created_by" to task.createdBy,
            ),
        )

        if (insertedRows == 0) throw DbException("Failed to create task")

        return findById(task.id) ?: throw NotFoundException("Task not found after creation")
    }

    override fun findById(id: String): Task? {
        val selectQuery = """
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy
            FROM tasks
            WHERE id = :id
        """

        return DatabaseUtils
            .get(
                handle,
                selectQuery,
                mapOf("id" to id),
                Task::class,
            ).firstOrNull()
    }
}
```

### Key Principles

- **Single responsibility**: Each repository handles one entity type
- **Interface segregation**: Repositories implement specific interfaces
- **Dependency inversion**: Handlers depend on repository interfaces, not implementations
- **Error handling**: Repositories throw appropriate exceptions for database errors

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
// All transactions are managed by the Unit of Work
unitOfWork.execute { context ->
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
val member = context.households.findMemberById(householdId, userId)
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
- Mock Unit of Work for handler testing

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

- Always use `DatabaseUtils` methods in repositories
- Handle `SQLIntegrityConstraintViolationException` as `ConflictException`
- Wrap unknown database errors in `DbException`
- Use Unit of Work for multi-step operations

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
2. **Create entity** in appropriate domain package
3. **Create repository interface** in `shared/UnitOfWork.kt`
4. **Create repository implementation** in domain package
5. **Update Unit of Work** to include new repository
6. **Create handler** with Request/Response data classes
7. **Create controller** with route registration
8. **Add to Koin module** in `AppModule.kt`
9. **Add to routing** in `Routing.kt`
10. **Add tests** for new functionality

### Example: Adding a New Resource

```kotlin
// 1. Create entity
data class NewResource(
    val id: String,
    val name: String,
    val description: String,
)

// 2. Add repository interface to UnitOfWork.kt
interface NewResourceRepository {
    fun findById(id: String): NewResource?
    fun create(resource: NewResource): NewResource
    fun update(resource: NewResource): NewResource
}

// 3. Create repository implementation
class JdbiNewResourceRepository(
    private val handle: Handle,
) : NewResourceRepository {
    override fun findById(id: String): NewResource? {
        // Implementation
    }

    override fun create(resource: NewResource): NewResource {
        // Implementation
    }

    override fun update(resource: NewResource): NewResource {
        // Implementation
    }
}

// 4. Update UnitOfWorkContext
interface UnitOfWorkContext {
    // ... existing repositories
    val newResources: NewResourceRepository
}

// 5. Update JdbiUnitOfWorkContext
class JdbiUnitOfWorkContext(
    private val handle: Handle,
) : UnitOfWorkContext {
    // ... existing repositories
    override val newResources: NewResourceRepository = JdbiNewResourceRepository(handle)
}

// 6. Create handler
class NewResourceHandler(
    private val unitOfWork: UnitOfWork,
) {
    data class Request(val name: String, val description: String)
    data class Response(val id: String, val name: String, val description: String)

    suspend fun handle(request: Request): Response {
        return unitOfWork.execute { context ->
            val resource = context.newResources.create(
                NewResource(
                    id = UUID.randomUUID().toString(),
                    name = request.name,
                    description = request.description,
                )
            )

            Response(
                id = resource.id,
                name = resource.name,
                description = resource.description,
            )
        }
    }
}

// 7. Create controller
class NewResourceController(
    private val handler: NewResourceHandler,
) {
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

// 8. Add to AppModule.kt
single { NewResourceHandler(get()) }
single { NewResourceController(get()) }

// 9. Add to Routing.kt
val newResourceController: NewResourceController by inject()
newResourceController.apply {
    registerNewResourceRoutes()
}
```

This architecture ensures maintainability, testability, and consistency across the application. Always follow these patterns when making changes to maintain code quality and consistency.
