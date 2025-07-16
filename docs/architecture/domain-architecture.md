# Domain Architecture

## Overview

Ohana follows a layered architecture with Domain-Driven Design (DDD) principles, organizing code into clear layers and bounded contexts. The application uses a clean architecture approach with separate layers for API, domain logic, and data access.

## Project Structure

```
com.ohana/
├── api/                     # HTTP Controllers Layer
│   ├── auth/               # Authentication endpoints
│   ├── health/             # Health check endpoints
│   ├── household/          # Household endpoints
│   ├── member/             # Member endpoints
│   ├── task/               # Task endpoints
│   └── utils/              # API utilities
├── data/                   # Data Access Layer
│   ├── auth/               # Auth data access
│   ├── household/          # Household data access
│   ├── member/             # Member data access
│   ├── task/               # Task data access
│   ├── unitOfWork/         # Transaction management
│   └── utils/              # Data utilities
├── domain/                 # Business Logic Layer
│   ├── auth/               # Authentication business logic
│   ├── household/          # Household business logic
│   ├── member/             # Member business logic
│   ├── task/               # Task business logic
│   └── validators/         # Cross-domain validators
├── plugins/                # Ktor Application Plugins
├── shared/                 # Shared Components
│   ├── enums/             # Shared enums
│   ├── exceptions/        # Shared exceptions
│   └── Guid.kt            # UUID utilities
└── Application.kt         # Main application entry point
```

## Layer Details

### 1. API Layer (`api/`)

**Purpose**: Handles HTTP requests and responses, input validation, and routing

**Components**:

- `controllers/` - HTTP endpoints for each domain
- `utils/` - API utilities like `GetUserId.kt`

**Key Responsibilities**:

- HTTP request/response handling
- Input validation using Bean Validation
- Route registration
- Error response formatting

**Example Structure**:

```
api/
├── auth/AuthController.kt
├── household/HouseholdController.kt
├── member/MemberController.kt
├── task/TaskController.kt
└── utils/GetUserId.kt
```

### 2. Data Layer (`data/`)

**Purpose**: Manages data persistence and database operations

**Components**:

- `repositories/` - Data access interfaces and implementations
- `entities/` - Database entities
- `unitOfWork/` - Transaction management

**Key Responsibilities**:

- Database operations
- Entity mapping
- Transaction management
- Data persistence

**Example Structure**:

```
data/
├── auth/
│   ├── AuthMember.kt
│   ├── AuthMemberRepository.kt
│   └── JdbiAuthMemberRepository.kt
├── household/
│   ├── Household.kt
│   ├── HouseholdMember.kt
│   ├── HouseholdRepository.kt
│   └── JdbiHouseholdRepository.kt
└── unitOfWork/
    ├── UnitOfWork.kt
    └── JdbiUnitOfWork.kt
```

### 3. Domain Layer (`domain/`)

**Purpose**: Contains business logic and domain rules

**Components**:

- `handlers/` - Business logic for domain operations
- `validators/` - Cross-domain validation logic

**Key Responsibilities**:

- Business rule enforcement
- Domain logic implementation
- Cross-domain validation
- Use case orchestration

**Example Structure**:

```
domain/
├── auth/
│   ├── MemberRegistrationHandler.kt
│   ├── MemberSignInHandler.kt
│   └── utils/
│       ├── Hasher.kt
│       └── JwtCreator.kt
├── household/
│   ├── HouseholdCreationHandler.kt
│   ├── HouseholdInviteMemberHandler.kt
│   └── HouseholdAcceptInviteHandler.kt
└── validators/
    ├── FutureDate.kt
    └── HouseholdMemberValidator.kt
```

### 4. Plugins Layer (`plugins/`)

**Purpose**: Ktor application configuration and middleware

**Components**:

- `AppConfig.kt` - Application configuration
- `AppModule.kt` - Dependency injection setup
- `Security.kt` - Authentication and authorization
- `Validation.kt` - Request validation
- `ExceptionHandling.kt` - Error handling
- `Routing.kt` - Route registration
- `Serialization.kt` - JSON serialization
- `ConfigureCORS.kt` - CORS configuration
- `RateLimit.kt` - Rate limiting
- `CallLogging.kt` - Request logging

### 5. Shared Components (`shared/`)

**Purpose**: Components used across multiple layers

**Components**:

- `enums/` - Shared enumerations
- `exceptions/` - Custom exception classes
- `Guid.kt` - UUID validation utilities

## Domain Details

### 1. Auth Domain

**Purpose**: Handles user authentication and registration

**API Layer**:

- `AuthController.kt` - Login and registration endpoints

**Data Layer**:

- `AuthMember.kt` - User entity with credentials
- `AuthMemberRepository.kt` - Data access interface
- `JdbiAuthMemberRepository.kt` - JDBI implementation

**Domain Layer**:

- `MemberRegistrationHandler.kt` - Registration business logic
- `MemberSignInHandler.kt` - Login business logic
- `utils/Hasher.kt` - Password hashing
- `utils/JwtCreator.kt` - JWT token generation

**Key Operations**:

- User registration with validation
- User login with JWT token generation
- Password hashing and validation

### 2. Member Domain

**Purpose**: Manages user profiles and information

**API Layer**:

- `MemberController.kt` - Profile management endpoints

**Data Layer**:

- `Member.kt` - User profile entity
- `MemberRepository.kt` - Data access interface
- `JdbiMemberRepository.kt` - JDBI implementation

**Domain Layer**:

- `MemberGetAllHandler.kt` - List all members
- `MemberGetByIdHandler.kt` - Get member by ID
- `MemberUpdateByIdHandler.kt` - Update member profile

**Key Operations**:

- Get user profile
- Update user profile
- List all members

### 3. Household Domain

**Purpose**: Manages household creation and membership

**API Layer**:

- `HouseholdController.kt` - Household operation endpoints

**Data Layer**:

- `Household.kt` - Household entity
- `HouseholdMember.kt` - Membership relationship
- `HouseholdRepository.kt` - Data access interface
- `JdbiHouseholdRepository.kt` - JDBI implementation

**Domain Layer**:

- `HouseholdCreationHandler.kt` - Create household
- `HouseholdInviteMemberHandler.kt` - Invite members
- `HouseholdAcceptInviteHandler.kt` - Accept invitations
- `HouseholdGetAllHandler.kt` - List households
- `HouseholdGetByIdHandler.kt` - Get household details

**Key Operations**:

- Create household
- Invite members to household
- Accept household invitations
- Get household information
- Manage household membership

### 4. Task Domain

**Purpose**: Manages task creation and tracking

**API Layer**:

- `TaskController.kt` - Task operation endpoints

**Data Layer**:

- `Task.kt` - Task entity
- `TaskRepository.kt` - Data access interface
- `JdbiTaskRepository.kt` - JDBI implementation

**Domain Layer**:

- `TaskCreationHandler.kt` - Create task
- `TaskUpdateByIdHandler.kt` - Update task
- `TaskGetAllHandler.kt` - List tasks
- `TaskGetByIdHandler.kt` - Get task details

**Key Operations**:

- Create task
- Update task
- Get tasks for household
- Change task status

## Data Flow Between Layers

```
HTTP Request → API Layer → Domain Layer → Data Layer → Database
                                                            ↓
HTTP Response ← API Layer ← Domain Layer ← Data Layer ← Database
```

### Layer Communication

1. **API → Domain**: Controllers call handlers with validated requests
2. **Domain → Data**: Handlers use Unit of Work to access repositories
3. **Data → Database**: Repositories execute SQL via JDBI
4. **Response Flow**: Data flows back through the same layers

## Unit of Work Pattern

The application uses the Unit of Work pattern for transaction management:

```kotlin
interface UnitOfWork {
    suspend fun <T> execute(block: (UnitOfWorkContext) -> T): T
}

interface UnitOfWorkContext {
    val tasks: TaskRepository
    val members: MemberRepository
    val households: HouseholdRepository
    val authMembers: AuthMemberRepository
}
```

This ensures:

- **Transaction consistency** across multiple repositories
- **Clean separation** between business logic and data access
- **Testability** through dependency injection

## Dependency Injection

The application uses Koin for dependency injection:

```kotlin
// In AppModule.kt
val appModule = module {
    // Repositories
    single<AuthMemberRepository> { JdbiAuthMemberRepository(get()) }
    single<MemberRepository> { JdbiMemberRepository(get()) }

    // Handlers
    single { MemberRegistrationHandler(get()) }
    single { MemberSignInHandler(get()) }

    // Controllers
    single { AuthController(get(), get()) }
}
```

## Validation Patterns

The application uses Bean Validation (JSR-303) for input validation:

```kotlin
data class Request(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 3, message = "Name must be at least 3 characters long")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)
```

## Testing Structure

Tests follow the same layered structure:

```
src/test/kotlin/com/ohana/
├── domain/                 # Domain layer tests
│   ├── auth/              # Auth handler tests
│   ├── household/         # Household handler tests
│   ├── member/            # Member handler tests
│   ├── task/              # Task handler tests
│   └── shared/            # Shared validator tests
└── TestUtils.kt           # Test utilities
```

## Adding New Features

### Steps to Add a New Domain

1. **Create API Layer**:

   ```
   api/newdomain/NewController.kt
   ```

2. **Create Data Layer**:

   ```
   data/newdomain/
   ├── NewEntity.kt
   ├── NewRepository.kt
   └── JdbiNewRepository.kt
   ```

3. **Create Domain Layer**:

   ```
   domain/newdomain/
   ├── NewCreationHandler.kt
   ├── NewGetHandler.kt
   └── utils/NewValidator.kt
   ```

4. **Update Unit of Work**:

   ```kotlin
   interface UnitOfWorkContext {
       val newEntities: NewRepository
       // ... existing repositories
   }
   ```

5. **Add to Dependency Injection**:

   ```kotlin
   // In AppModule.kt
   single<NewRepository> { JdbiNewRepository(get()) }
   single { NewCreationHandler(get()) }
   single { NewController(get()) }
   ```

6. **Register Routes**:
   ```kotlin
   // In Routing.kt
   newController.registerNewRoutes()
   ```

## Best Practices

### 1. Layer Separation

- ✅ Keep business logic in domain layer
- ✅ Keep data access in data layer
- ✅ Keep HTTP handling in API layer
- ❌ Don't mix concerns across layers

### 2. Dependency Direction

Dependencies should flow inward:

- ✅ API → Domain → Data
- ❌ Data → Domain → API (creates circular dependencies)

### 3. Validation

- ✅ Use Bean Validation for input validation
- ✅ Keep domain validation in domain layer
- ✅ Use custom validators for complex rules

### 4. Error Handling

- ✅ Use custom exceptions in shared layer
- ✅ Handle exceptions in plugins layer
- ✅ Return appropriate HTTP status codes

### 5. Testing

- ✅ Test each layer independently
- ✅ Use mocks for dependencies
- ✅ Test business logic thoroughly

## Summary

The layered architecture provides:

- **Clear separation** of concerns across layers
- **Maintainable** code organization
- **Testable** components with dependency injection
- **Scalable** structure for adding new features
- **Consistent** patterns across all domains
- **Transaction safety** through Unit of Work pattern

Each layer has a specific responsibility and communicates with other layers through well-defined interfaces, making the codebase modular and easy to maintain.
