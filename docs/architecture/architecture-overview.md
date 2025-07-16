# Ohana Backend Architecture Overview

## What is Ohana?

Ohana is a household management application built with Kotlin and Ktor. It allows families and groups to manage tasks, coordinate activities, and stay organized together.

## High-Level Architecture

### Technology Stack

- **Language**: Kotlin
- **Framework**: Ktor (async web framework)
- **Database**: MySQL with JDBI
- **Dependency Injection**: Koin
- **Authentication**: JWT tokens

### Core Principles

1. **Domain-Driven Design**: The system is organized into clear domains (Auth, Member, Household, Task)
2. **Layered Architecture**: Controllers → Handlers → Unit of Work → Repositories
3. **Clean Separation**: Each layer has a single responsibility
4. **Type Safety**: Strong typing throughout the application
5. **Testability**: Easy to test with dependency injection and mocking

### Architecture Layers

```
┌─────────────────┐
│   Controllers   │ ← HTTP requests/responses
├─────────────────┤
│    Handlers     │ ← Business logic
├─────────────────┤
│  Unit of Work   │ ← Transaction management
├─────────────────┤
│  Repositories   │ ← Data access
├─────────────────┤
│    Database     │ ← MySQL storage
└─────────────────┘
```

### Domain Organization

The application is divided into four main domains:

1. **Auth Domain**: User registration, login, and authentication
2. **Member Domain**: User profile management
3. **Household Domain**: Group creation and membership management
4. **Task Domain**: Task creation, assignment, and tracking

Each domain contains:

- Controllers (HTTP endpoints)
- Handlers (business logic)
- Repositories (data access)
- Entities (data models)

### Key Patterns

- **Unit of Work Pattern**: Centralized transaction management
- **Repository Pattern**: Abstracted data access
- **Request-Response Pattern**: Consistent API structure
- **Validation Pattern**: Input validation at multiple levels
- **Exception Handling**: Custom exception hierarchy

### Data Flow

1. **HTTP Request** → Controller
2. **Controller** → Validates input and calls Handler
3. **Handler** → Contains business logic and uses Unit of Work
4. **Unit of Work** → Manages database transactions
5. **Repository** → Performs actual database operations
6. **Response** → Returns data through the same chain

### Benefits of This Architecture

- **Maintainable**: Clear separation of concerns
- **Testable**: Easy to mock dependencies
- **Scalable**: Can add new domains without affecting existing ones
- **Consistent**: Same patterns used throughout
- **Type Safe**: Compile-time error checking

For detailed information about specific aspects, see:

- [Domain Architecture](domain-architecture.md) - How domains are organized and layered architecture
- [Data Access Patterns](data-access.md) - Repository and Unit of Work patterns
- [API Design](api-design.md) - HTTP endpoints and controllers
- [Entities](entities.md) - Data models and entity relationships
- [Security](security.md) - Authentication, authorization, and security patterns
- [Validation](validation.md) - Input validation and business rule enforcement
- [Configuration](configuration.md) - Environment setup and configuration
