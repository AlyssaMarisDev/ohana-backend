# Data Access Patterns

## Overview

Ohana uses the Unit of Work pattern combined with the Repository pattern to provide a clean, testable, and consistent way to access data. This approach separates business logic from data access concerns and ensures proper transaction management.

## Unit of Work Pattern

### What is Unit of Work?

The Unit of Work pattern provides a centralized way to manage database transactions and repository access. It ensures that all database operations within a single business operation are wrapped in a transaction.

### Core Interfaces

```kotlin
// Main Unit of Work interface
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

// Context that provides access to repositories
interface UnitOfWorkContext {
    val tasks: TaskRepository
    val members: MemberRepository
    val households: HouseholdRepository
    val authMembers: AuthMemberRepository
}
```

### How It Works

```kotlin
class TaskCreationHandler(
    private val unitOfWork: UnitOfWork,
) {
    suspend fun handle(userId: String, request: Request): Response =
        unitOfWork.execute { context ->
            // All operations within this block are in a single transaction

            // Validate user exists
            val user = context.members.findById(userId)
                ?: throw NotFoundException("User not found")

            // Create task
            val task = context.tasks.create(
                Task(
                    id = request.id,
                    title = request.title,
                    description = request.description,
                    dueDate = request.dueDate,
                    status = request.status,
                    createdBy = userId,
                    householdId = request.householdId,
                )
            )

            // Return response
            Response(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                status = task.status,
                createdBy = task.createdBy,
                householdId = task.householdId,
            )
        }
}
```

### Benefits

1. **Automatic Transaction Management**: All operations are wrapped in a transaction
2. **Automatic Rollback**: If any operation fails, all changes are rolled back
3. **Consistent Error Handling**: Database errors are converted to appropriate exceptions
4. **Testability**: Easy to mock for unit testing
5. **Type Safety**: Strongly typed repository interfaces

## Repository Pattern

### Repository Interfaces

Each entity has a corresponding repository interface that defines the data access contract:

```kotlin
interface TaskRepository {
    fun create(task: Task): Task
    fun findById(id: String): Task?
    fun findAll(): List<Task>
    fun findByHouseholdId(householdId: String): List<Task>
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

### Repository Implementation

Repositories are implemented using JDBI and follow a consistent pattern:

```kotlin
class JdbiTaskRepository(
    private val handle: Handle,
) : TaskRepository {

    override fun create(task: Task): Task {
        val insertQuery = """
            INSERT INTO tasks (id, title, description, due_date, status, created_by, household_id)
            VALUES (:id, :title, :description, :due_date, :status, :created_by, :household_id)
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
                "household_id" to task.householdId,
            ),
        )

        if (insertedRows == 0) throw DbException("Failed to create task")

        return findById(task.id) ?: throw NotFoundException("Task not found after creation")
    }

    override fun findById(id: String): Task? {
        val selectQuery = """
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy, household_id as householdId
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

    override fun findByHouseholdId(householdId: String): List<Task> {
        val selectQuery = """
            SELECT id, title, description, due_date as dueDate, status, created_by as createdBy, household_id as householdId
            FROM tasks
            WHERE household_id = :household_id
            ORDER BY created_at DESC
        """

        return DatabaseUtils.get(
            handle,
            selectQuery,
            mapOf("household_id" to householdId),
            Task::class,
        )
    }

    override fun update(task: Task): Task {
        val updateQuery = """
            UPDATE tasks
            SET title = :title, description = :description, due_date = :due_date, status = :status
            WHERE id = :id
        """

        val updatedRows = DatabaseUtils.update(
            handle,
            updateQuery,
            mapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "due_date" to task.dueDate,
                "status" to task.status.name,
            ),
        )

        if (updatedRows == 0) throw NotFoundException("Task not found for update")

        return findById(task.id) ?: throw NotFoundException("Task not found after update")
    }
}
```

## Database Utilities

### DatabaseUtils Pattern

All database operations use the `DatabaseUtils` class for consistency:

```kotlin
object DatabaseUtils {
    fun insert(handle: Handle, query: String, params: Map<String, Any?>): Int {
        return handle.createUpdate(query)
            .bindMap(params)
            .execute()
    }

    fun update(handle: Handle, query: String, params: Map<String, Any?>): Int {
        return handle.createUpdate(query)
            .bindMap(params)
            .execute()
    }

    fun <T> get(handle: Handle, query: String, params: Map<String, Any?>, clazz: Class<T>): List<T> {
        return handle.createQuery(query)
            .bindMap(params)
            .mapTo(clazz)
            .list()
    }
}
```

### Query Patterns

#### Parameterized Queries

Always use parameterized queries to prevent SQL injection:

```kotlin
// ✅ Good - Parameterized query
val query = "SELECT * FROM tasks WHERE household_id = :household_id"
val params = mapOf("household_id" to householdId)

// ❌ Bad - String concatenation (SQL injection risk)
val query = "SELECT * FROM tasks WHERE household_id = '$householdId'"
```

#### Column Mapping

Use `as` aliases to map database columns to Kotlin properties:

```kotlin
val selectQuery = """
    SELECT
        id,
        title,
        description,
        due_date as dueDate,           -- snake_case to camelCase
        status,
        created_by as createdBy,       -- snake_case to camelCase
        household_id as householdId    -- snake_case to camelCase
    FROM tasks
    WHERE id = :id
"""
```

## Transaction Management

### Automatic Transactions

All operations within `unitOfWork.execute` are automatically wrapped in a transaction:

```kotlin
unitOfWork.execute { context ->
    // Transaction starts here

    val task = context.tasks.create(newTask)
    val member = context.members.update(updatedMember)

    // If any operation fails, all changes are rolled back
    // If all operations succeed, transaction is committed

    return response
}
```

### Manual Transaction Control

For complex scenarios, you can control transactions manually:

```kotlin
// This is handled automatically by the Unit of Work implementation
// You don't need to manage transactions manually in handlers
```

## Error Handling

### Repository Error Handling

Repositories throw appropriate exceptions:

```kotlin
override fun create(task: Task): Task {
    val insertedRows = DatabaseUtils.insert(handle, query, params)

    if (insertedRows == 0) {
        throw DbException("Failed to create task")
    }

    return findById(task.id) ?: throw NotFoundException("Task not found after creation")
}
```

### Exception Hierarchy

- `DbException` - General database errors
- `NotFoundException` - Entity not found
- `ConflictException` - Duplicate key or constraint violation
- `ValidationException` - Data validation errors

## Testing Data Access

### Mocking Unit of Work

```kotlin
@Test
fun `should create task when validation passes`() = runTest {
    // Mock the unit of work to execute the provided block
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Mock repository responses
    whenever(taskRepository.create(any())).thenReturn(task)

    val response = handler.handle(userId, request)

    // Verify repository was called
    verify(taskRepository).create(task)
}
```

### TestUtils Helper

```kotlin
object TestUtils {
    suspend fun mockUnitOfWork(
        unitOfWork: UnitOfWork,
        context: UnitOfWorkContext,
    ) {
        whenever(unitOfWork.execute<Any>(any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(UnitOfWorkContext) -> Any>(0)
            block.invoke(context)
        }
    }
}
```

## Best Practices

### 1. Always Use Unit of Work

```kotlin
// ✅ Good - Use Unit of Work
suspend fun handle(request: Request): Response =
    unitOfWork.execute { context ->
        val entity = context.repository.findById(id)
        // ... business logic
    }

// ❌ Bad - Direct repository access
suspend fun handle(request: Request): Response {
    val entity = repository.findById(id) // No transaction management
    // ... business logic
}
```

### 2. Repository Interface Segregation

```kotlin
// ✅ Good - Specific interface
interface TaskRepository {
    fun findById(id: String): Task?
    fun findByHouseholdId(householdId: String): List<Task>
}

// ❌ Bad - Generic interface
interface Repository<T> {
    fun findById(id: String): T?
    fun findAll(): List<T>
}
```

### 3. Consistent Error Handling

```kotlin
// ✅ Good - Consistent error handling
override fun findById(id: String): Task? {
    return DatabaseUtils.get(handle, query, params, Task::class).firstOrNull()
}

// ❌ Bad - Inconsistent error handling
override fun findById(id: String): Task? {
    try {
        return handle.createQuery(query).bind("id", id).mapTo(Task::class.java).first()
    } catch (e: Exception) {
        logger.error("Error finding task", e)
        return null
    }
}
```

### 4. Use DatabaseUtils

```kotlin
// ✅ Good - Use DatabaseUtils
val result = DatabaseUtils.get(handle, query, params, Entity::class)

// ❌ Bad - Direct JDBI usage
val result = handle.createQuery(query).bindMap(params).mapTo(Entity::class).list()
```

## Adding New Repositories

### Steps to Add a New Repository

1. **Define Interface**:

   ```kotlin
   interface NewRepository {
       fun findById(id: String): NewEntity?
       fun create(entity: NewEntity): NewEntity
       fun update(entity: NewEntity): NewEntity
   }
   ```

2. **Add to Unit of Work**:

   ```kotlin
   interface UnitOfWorkContext {
       val newEntities: NewRepository
       // ... existing repositories
   }
   ```

3. **Implement Repository**:

   ```kotlin
   class JdbiNewRepository(
       private val handle: Handle,
   ) : NewRepository {
       // Implementation using DatabaseUtils
   }
   ```

4. **Register in DI**:
   ```kotlin
   single<NewRepository> { JdbiNewRepository(get()) }
   ```

## Summary

The data access patterns provide:

- **Consistent** transaction management
- **Testable** data access layer
- **Type-safe** repository interfaces
- **Clean separation** of concerns
- **Proper error handling**

This approach ensures that all database operations are properly managed and that the business logic layer doesn't need to worry about transaction details.
