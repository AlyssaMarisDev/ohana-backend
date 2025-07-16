# Ohana Backend Testing Patterns

## Overview

This document outlines the testing patterns and conventions used in the Ohana Backend project. The project uses JUnit 5 with Mockito-Kotlin for unit testing, and follows specific patterns for testing handlers, repositories, and controllers.

## Test Structure and Organization

### Package Structure

Tests follow the same package structure as the main code:

```
src/test/kotlin/com/ohana/
├── auth/
│   ├── handlers/
│   └── controllers/
├── household/
│   ├── handlers/
│   └── controllers/
├── member/
│   ├── handlers/
│   └── controllers/
├── task/
│   ├── handlers/
│   └── controllers/
└── utils/
    └── TestUtils.kt
```

### Test Class Naming

- **Handler Tests**: `{HandlerName}Test.kt` (e.g., `TaskCreationHandlerTest.kt`)
- **Controller Tests**: `{ControllerName}Test.kt` (e.g., `TaskControllerTest.kt`)
- **Repository Tests**: `{RepositoryName}Test.kt` (e.g., `JdbiTaskRepositoryTest.kt`)

## Handler Testing Pattern

### Test Class Structure

```kotlin
class TaskCreationHandlerTest {
    // Dependencies
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var taskRepository: TaskRepository
    private lateinit var memberRepository: MemberRepository
    private lateinit var handler: TaskCreationHandler

    // Test data
    private val userId = "user-1"

    @BeforeEach
    fun setUp() {
        // Initialize mocks
        taskRepository = mock()
        memberRepository = mock()
        context = mock {
            on { tasks } doReturn taskRepository
            on { members } doReturn memberRepository
        }
        unitOfWork = mock()
        handler = TaskCreationHandler(unitOfWork)
    }

    @Test
    fun `test description`() = runTest {
        // Test implementation
    }
}
```

### Key Testing Patterns

#### 1. Mock Setup Pattern

```kotlin
@BeforeEach
fun setUp() {
    // Create repository mocks
    taskRepository = mock()
    memberRepository = mock()

    // Create context mock with repository assignments
    context = mock {
        on { tasks } doReturn taskRepository
        on { members } doReturn memberRepository
    }

    // Create unit of work mock
    unitOfWork = mock()

    // Create handler with mocked dependencies
    handler = TaskCreationHandler(unitOfWork)
}
```

#### 2. Unit of Work Mocking Pattern

```kotlin
@Test
fun `handle should create task when user exists`() = runTest {
    // Mock the unitOfWork.execute to actually execute the provided block
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Test implementation
    val response = handler.handle(userId, request)

    // Assertions
    assertEquals(expectedValue, response.someProperty)
}
```

#### 3. Repository Mocking Pattern

```kotlin
@Test
fun `handle should create task when user exists`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Mock repository responses
    whenever(memberRepository.findById(userId)).thenReturn(member)
    whenever(taskRepository.create(any())).thenReturn(task)

    val response = handler.handle(userId, request)

    // Verify repository calls
    verify(memberRepository).findById(userId)
    verify(taskRepository).create(task)
}
```

#### 4. Exception Testing Pattern

```kotlin
@Test
fun `handle should throw when user does not exist`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Mock repository to return null (user not found)
    whenever(memberRepository.findById(userId)).thenReturn(null)

    // Assert exception is thrown
    val ex = assertThrows<IllegalArgumentException> {
        handler.handle(userId, request)
    }
    assertEquals("User not found", ex.message)
}
```

#### 5. Exception Propagation Testing Pattern

```kotlin
@Test
fun `handle should propagate exception from repository`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Mock repository to throw exception
    whenever(memberRepository.findById(userId)).thenReturn(member)
    whenever(taskRepository.create(any())).thenThrow(RuntimeException("DB error"))

    // Assert exception is propagated
    val ex = assertThrows<RuntimeException> {
        handler.handle(userId, request)
    }
    assertEquals("DB error", ex.message)
}
```

#### 6. Void Method Exception Testing Pattern

```kotlin
@Test
fun `handle should throw when validator fails`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Mock void method to throw exception
    whenever(validator.validate(context, id, userId)).thenThrow(AuthorizationException("User not authorized"))

    // Assert exception is thrown
    val ex = assertThrows<AuthorizationException> {
        handler.handle(id, userId)
    }
    assertEquals("User not authorized", ex.message)
}
```

#### 7. Void Method Success Testing Pattern

```kotlin
@Test
fun `handle should succeed when validator passes`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    // Don't need to mock void method success

    val response = handler.handle(id, userId)

    // Assert successful response
    assertEquals(expectedValue, response.property)
    verify(validator).validate(context, id, userId)
}
```

### Important Notes

- **Always set up mocks in @BeforeEach**: This ensures mocks aren't shared between test cases
- **Always mock dependencies passed into the class**: This ensures we aren't duplicating testing effort since dependencies should have their own tests

## Test Data Management

### TestUtils Pattern

Add to the `TestUtils.kt` file for reusable test data:

```kotlin
object TestUtils {
    fun getTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        description: String = "Test Description",
        dueDate: Instant = Instant.now(),
        status: TaskStatus = TaskStatus.pending,
        createdBy: String = UUID.randomUUID().toString(),
    ): Task = Task(id, title, description, dueDate, status, createdBy)

    fun getMember(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test User",
        email: String = "test@example.com",
        age: Int? = null,
        gender: String? = null,
    ): Member = Member(id, name, email, age, gender)

    fun mockUnitOfWork(unitOfWork: UnitOfWork, context: UnitOfWorkContext) {
        whenever(unitOfWork.execute<Any>(any())).thenAnswer { invocation ->
            val block = invocation.getArgument<(UnitOfWorkContext) -> Any>(0)
            block.invoke(context)
        }
    }
}
```

### Inline Test Data Pattern

For test-specific data, create objects inline:

```kotlin
@Test
fun `handle should create task when user exists`() = runTest {
    TestUtils.mockUnitOfWork(unitOfWork, context)

    val request = TaskCreationHandler.Request(
        id = UUID.randomUUID().toString(),
        title = "Test Task",
        description = "Test Description",
        dueDate = Instant.now(),
        status = TaskStatus.pending,
    )
    val member = TestUtils.getMember()

    // Test implementation
}
```

## Coroutine Testing Pattern

### Using runTest

All tests that involve suspend functions must use `runTest`:

```kotlin
import kotlinx.coroutines.test.runTest

@Test
fun `test suspend function`() = runTest {
    // Test suspend function calls
    val result = handler.handle(userId, request)

    // Assertions
    assertEquals(expectedValue, result)
}
```

### Important Notes

- **Do NOT make @BeforeEach suspend**: JUnit 5 doesn't support suspend functions in lifecycle methods
- **Always use runTest**: For any test that calls suspend functions
- **Mock Unit of Work properly**: Use `TestUtils.mockUnitOfWork()` to ensure the execute block is actually called

## Assertion Patterns

### Response Validation Pattern

```kotlin
@Test
fun `handle should create task when user exists`() = runTest {
    // Setup and execution
    val response = handler.handle(userId, request)

    // Comprehensive response validation
    assertEquals(task.id, response.id)
    assertEquals(task.title, response.title)
    assertEquals(task.description, response.description)
    assertEquals(task.dueDate, response.dueDate)
    assertEquals(task.status, response.status)
    assertEquals(task.createdBy, response.createdBy)
}
```

### Exception Assertion Pattern

```kotlin
@Test
fun `handle should throw when user does not exist`() = runTest {
    // Setup

    val ex = assertThrows<IllegalArgumentException> {
        handler.handle(userId, request)
    }
    assertEquals("User not found", ex.message)
}
```

### Verification Pattern

```kotlin
@Test
fun `handle should call repositories correctly`() = runTest {
    // Setup and execution
    val response = handler.handle(userId, request)

    // Verify repository interactions
    verify(memberRepository).findById(userId)
    verify(taskRepository).create(task)

    // Verify no other interactions
    verifyNoMoreInteractions(memberRepository, taskRepository)
}
```

## Test Method Naming Convention

Use descriptive test names that explain the scenario and expected outcome:

```kotlin
@Test
fun `handle should create task when user exists`() = runTest { ... }

@Test
fun `handle should throw when user does not exist`() = runTest { ... }

@Test
fun `handle should propagate exception from repository`() = runTest { ... }

@Test
fun `handle should validate request parameters`() = runTest { ... }

@Test
fun `handle should return correct response format`() = runTest { ... }
```

## Import Patterns

### Required Imports

```kotlin
import com.ohana.TestUtils
import com.ohana.shared.MemberRepository
import com.ohana.shared.TaskRepository
import com.ohana.shared.TaskStatus
import com.ohana.shared.UnitOfWork
import com.ohana.shared.UnitOfWorkContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
```

### Key Import Notes

- Use `org.mockito.kotlin.*` for Mockito-Kotlin
- Use `kotlinx.coroutines.test.runTest` for coroutine testing
- Use `org.junit.jupiter.api.assertThrows` for exception testing
- Import specific assertion methods rather than using wildcards

## Test Categories

### 1. Happy Path Tests

Test successful execution with valid inputs:

```kotlin
@Test
fun `handle should create task when user exists`() = runTest {
    // Valid input, successful execution
    val response = handler.handle(userId, validRequest)

    // Assert successful response
    assertNotNull(response)
    assertEquals(expectedValue, response.property)
}
```

### 2. Validation Tests

Test input validation and error handling:

```kotlin
@Test
fun `handle should throw when user does not exist`() = runTest {
    // Invalid input (user not found)
    val ex = assertThrows<IllegalArgumentException> {
        handler.handle(userId, request)
    }
    assertEquals("User not found", ex.message)
}
```

### 3. Exception Propagation Tests

Test that exceptions from dependencies are properly propagated:

```kotlin
@Test
fun `handle should propagate exception from repository`() = runTest {
    // Mock repository to throw exception
    whenever(repository.method()).thenThrow(RuntimeException("DB error"))

    // Assert exception is propagated
    val ex = assertThrows<RuntimeException> {
        handler.handle(userId, request)
    }
    assertEquals("DB error", ex.message)
}

@Test
fun `handle should propagate exception from validator`() = runTest {
    // Mock validator to throw exception
    whenever(validator.validate(context, id, userId)).thenThrow(AuthorizationException("User not authorized"))

    // Assert exception is propagated
    val ex = assertThrows<AuthorizationException> {
        handler.handle(id, userId)
    }
    assertEquals("User not authorized", ex.message)
}
```

### 4. Edge Case Tests

Test boundary conditions and edge cases:

```kotlin
@Test
fun `handle should handle empty result set`() = runTest {
    // Mock repository to return empty list
    whenever(repository.findAll()).thenReturn(emptyList())

    val response = handler.handle(request)

    // Assert empty response
    assertTrue(response.items.isEmpty())
}
```

## Best Practices

### 1. Test Isolation

- Each test should be independent
- Use `@BeforeEach` for common setup
- Avoid shared state between tests

### 2. Mock Management

- Always mock dependencies passed into the class
- Use `verify()` to ensure expected interactions
- Use `verifyNoMoreInteractions()` when appropriate

### 3. Test Data

- Use `TestUtils` for reusable test data
- Create specific test data inline when needed
- Use realistic but simple test data

### 4. Assertions

- Be specific in assertions
- Test both positive and negative cases
- Verify all relevant properties of responses

### 5. Exception Testing

- Test both expected and unexpected exceptions
- Verify exception messages
- Test exception propagation from dependencies
- Use `whenever(method()).thenThrow(exception)` for void methods that throw exceptions
- Don't need to mock void methods that succeed
- Avoid using `doThrow().whenever()` pattern - prefer `whenever().thenThrow()`

### 6. Performance

- Keep tests fast
- Avoid unnecessary database calls in unit tests
- Use mocks for external dependencies

## Common Anti-Patterns to Avoid

### 1. Testing Implementation Details

```kotlin
// ❌ Don't test internal implementation
verify(internalHelper).someInternalMethod()

// ✅ Test public behavior
assertEquals(expectedResponse, actualResponse)
```

### 2. Over-Mocking

```kotlin
// ❌ Don't mock everything
whenever(simpleUtility.add(1, 2)).thenReturn(3)

// ✅ Only mock external dependencies
whenever(databaseRepository.findById(id)).thenReturn(entity)
```

### 3. Brittle Tests

```kotlin
// ❌ Don't test exact string matches that might change
assertEquals("Specific error message", exception.message)

// ✅ Test for presence of key information
assertTrue(exception.message?.contains("User not found") == true)
```

### 4. Testing Framework Code

```kotlin
// ❌ Don't test framework functionality
verify(ktorCall).respond(any())

// ✅ Test your business logic
assertEquals(expectedResponse, handler.handle(request))
```

This testing pattern ensures comprehensive coverage of the application while maintaining test readability, maintainability, and reliability.

## Quick Reference for AI Assistants

When writing tests for this project, follow these key steps:

1. **Import the required dependencies** (see Import Patterns section)
2. **Set up mocks in @BeforeEach** (see Mock Setup Pattern)
3. **Use TestUtils.mockUnitOfWork()** in each test method if needed
4. **Wrap test methods with runTest { }** for suspend functions
5. **Use descriptive test names** with backticks
6. **Mock repository responses** with whenever()
7. **Verify interactions** with verify()
8. **Test exceptions** with assertThrows
9. **Use TestUtils.getTask()/getMember()** for test data

### Example Template for New Handler Tests:

```kotlin
class NewHandlerTest {
    private lateinit var unitOfWork: UnitOfWork
    private lateinit var context: UnitOfWorkContext
    private lateinit var repository: SomeRepository
    private lateinit var handler: NewHandler

    @BeforeEach
    fun setUp() {
        repository = mock()
        context = mock {
            on { someRepository } doReturn repository
        }
        unitOfWork = mock()
        handler = NewHandler(unitOfWork)
    }

    @Test
    fun `handle should do something when condition is met`() = runTest {
        TestUtils.mockUnitOfWork(unitOfWork, context)

        // Mock repository responses
        whenever(repository.method()).thenReturn(result)

        val response = handler.handle(request)

        // Assertions
        assertEquals(expected, response.property)
        verify(repository).method()
    }
}
```

### Common Test Scenarios:

- **Success case**: Mock successful repository calls, verify response
- **Validation failure**: Mock repository to return null/empty, assert exception
- **Database error**: Mock repository to throw exception, assert propagation
- **Edge cases**: Test boundary conditions and empty results
