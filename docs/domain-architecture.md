# Domain Architecture

## Overview

Ohana follows Domain-Driven Design (DDD) principles, organizing code into clear, bounded contexts. Each domain represents a specific business area with its own entities, business logic, and data access.

## Domain Structure

```
com.ohana/
├── auth/                    # Authentication Domain
├── member/                  # Member Domain
├── household/               # Household Domain
├── task/                    # Task Domain
├── shared/                  # Shared Components
└── utils/                   # Shared Utilities
```

## Domain Details

### 1. Auth Domain (`auth/`)

**Purpose**: Handles user authentication and registration

**Components**:

- `controllers/` - HTTP endpoints for login/registration
- `handlers/` - Business logic for auth operations
- `entities/` - AuthMember (user with credentials)
- `repositories/` - Data access for auth data
- `utils/` - JWT, password hashing utilities

**Key Operations**:

- User registration
- User login
- JWT token generation
- Password hashing and validation

**Entities**:

- `AuthMember` - User account with authentication details

### 2. Member Domain (`member/`)

**Purpose**: Manages user profiles and information

**Components**:

- `controllers/` - HTTP endpoints for profile management
- `handlers/` - Business logic for member operations
- `entities/` - Member (user profile without auth details)
- `repositories/` - Data access for member data

**Key Operations**:

- Get user profile
- Update user profile
- List all members

**Entities**:

- `Member` - User profile information

### 3. Household Domain (`household/`)

**Purpose**: Manages household creation and membership

**Components**:

- `controllers/` - HTTP endpoints for household operations
- `handlers/` - Business logic for household management
- `repositories/` - Data access for household data

**Key Operations**:

- Create household
- Invite members to household
- Accept household invitations
- Get household information
- Manage household membership

**Entities**:

- `Household` - Group/household information
- `HouseholdMember` - Membership relationship
- `HouseholdMemberRole` - Role enum (admin, member)

### 4. Task Domain (`task/`)

**Purpose**: Manages task creation and tracking

**Components**:

- `controllers/` - HTTP endpoints for task operations
- `handlers/` - Business logic for task management
- `entities/` - Task entity
- `repositories/` - Data access for task data

**Key Operations**:

- Create task
- Update task
- Get tasks for household
- Change task status

**Entities**:

- `Task` - Task information
- `TaskStatus` - Status enum (pending, in_progress, completed)

## Shared Components (`shared/`)

**Purpose**: Components used across multiple domains

**Components**:

- `UnitOfWork.kt` - Transaction management
- `UnitOfWorkContext` - Repository access interface
- Repository interfaces
- Shared enums and constants
- `Guid.kt` - UUID validation utilities

## Domain Relationships

### Data Flow Between Domains

```
Auth Domain     Member Domain
     ↓               ↓
     └───────┬───────┘
             ↓
    Household Domain
             ↓
      Task Domain
```

### Entity Relationships

1. **AuthMember ↔ Member**: One-to-one relationship (same ID)
2. **Member → HouseholdMember**: One-to-many (member can be in multiple households)
3. **Household → HouseholdMember**: One-to-many (household has multiple members)
4. **Member → Task**: One-to-many (member can create multiple tasks)
5. **Household → Task**: One-to-many (household has multiple tasks)

### Cross-Domain Dependencies

- **Household Domain** depends on **Member Domain** (for member validation)
- **Task Domain** depends on **Household Domain** (for household validation)
- **All Domains** depend on **Shared Components** (Unit of Work, repositories)

## Domain Boundaries

### Clear Boundaries

Each domain has clear responsibilities:

- **Auth Domain**: Only handles authentication and registration
- **Member Domain**: Only handles profile management
- **Household Domain**: Only handles household and membership operations
- **Task Domain**: Only handles task operations

### Cross-Domain Communication

Domains communicate through:

1. **Shared Entities**: Common data structures
2. **Repository Interfaces**: Data access contracts
3. **Unit of Work**: Transaction management
4. **Validation**: Cross-domain business rules

## Adding New Domains

### Steps to Add a New Domain

1. **Create Domain Package**:

   ```
   com.ohana/newdomain/
   ├── controllers/
   ├── handlers/
   ├── entities/
   └── repositories/
   ```

2. **Define Entities**:

   ```kotlin
   data class NewEntity(
       val id: String,
       val name: String,
       // ... other fields
   )
   ```

3. **Add Repository Interface**:

   ```kotlin
   interface NewRepository {
       fun findById(id: String): NewEntity?
       fun create(entity: NewEntity): NewEntity
       // ... other methods
   }
   ```

4. **Update Unit of Work**:

   ```kotlin
   interface UnitOfWorkContext {
       val newEntities: NewRepository
       // ... existing repositories
   }
   ```

5. **Create Handler**:

   ```kotlin
   class NewHandler(
       private val unitOfWork: UnitOfWork,
   ) {
       // Business logic
   }
   ```

6. **Create Controller**:
   ```kotlin
   class NewController(
       private val handler: NewHandler,
   ) {
       fun Route.registerNewRoutes() {
           // HTTP endpoints
       }
   }
   ```

## Domain Best Practices

### 1. Single Responsibility

Each domain should have one clear purpose:

- ✅ Auth domain only handles authentication
- ❌ Don't mix authentication with task management

### 2. Encapsulation

Keep domain-specific logic within the domain:

- ✅ Business rules in handlers
- ❌ Don't expose internal domain logic to other domains

### 3. Dependency Direction

Dependencies should flow toward shared components:

- ✅ Domain → Shared
- ❌ Shared → Domain (creates circular dependencies)

### 4. Entity Ownership

Each entity belongs to one primary domain:

- ✅ Task entity in task domain
- ❌ Don't duplicate entities across domains

### 5. Repository Boundaries

Repositories should only access their domain's data:

- ✅ TaskRepository only accesses task table
- ❌ Don't mix data access across domains

## Common Patterns

### Domain Service Pattern

For complex business logic that doesn't fit in handlers:

```kotlin
class HouseholdMemberValidator {
    fun validate(context: UnitOfWorkContext, householdId: String, userId: String) {
        // Complex validation logic
    }
}
```

### Domain Event Pattern

For cross-domain communication (future enhancement):

```kotlin
// When a household is created, notify other domains
class HouseholdCreatedEvent(val householdId: String, val createdBy: String)
```

### Value Objects

For domain-specific concepts:

```kotlin
// Instead of primitive types
data class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
    }
}
```

## Testing Domains

### Unit Testing

Test each domain in isolation:

```kotlin
class TaskHandlerTest {
    @Test
    fun `should create task when user is household member`() {
        // Test only task domain logic
    }
}
```

### Integration Testing

Test domain interactions:

```kotlin
class HouseholdTaskIntegrationTest {
    @Test
    fun `should create task only for household members`() {
        // Test household + task domain interaction
    }
}
```

## Summary

The domain architecture provides:

- **Clear separation** of business concerns
- **Maintainable** code organization
- **Testable** components
- **Scalable** structure for adding new features
- **Consistent** patterns across domains

Each domain is self-contained but can interact with others through well-defined interfaces and shared components.
