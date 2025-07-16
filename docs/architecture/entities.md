# Ohana Backend Entities

## Overview

The Ohana backend system is a household management application built with Kotlin and Ktor. It follows a domain-driven design with clear separation between authentication, member management, household management, and task management domains.

## Core Entities

### Authentication Domain

#### AuthMember

- **Purpose**: Represents a user account with authentication credentials
- **Key Fields**:
  - `id`: UUID string for unique identification
  - `name`: Full name of the user
  - `email`: Unique email address (used for login)
  - `password`: Hashed password for security
  - `salt`: Cryptographic salt used for password hashing
  - `age`: Optional age field
  - `gender`: Optional gender field
- **Database Table**: `members` (shared with Member entity)
- **Relationships**: One-to-one with Member entity (same ID)

### Member Domain

#### Member

- **Purpose**: Represents a user's profile information without authentication details
- **Key Fields**:
  - `id`: UUID string for unique identification
  - `name`: Full name of the member
  - `email`: Email address (unique)
  - `age`: Optional age field
  - `gender`: Optional gender field
- **Database Table**: `members` (shared with AuthMember entity)
- **Relationships**:
  - One-to-one with AuthMember entity (same ID)
  - One-to-many with HouseholdMember (as member)
  - One-to-many with Task (as creator)

### Household Domain

#### Household

- **Purpose**: Represents a household/group that members can belong to
- **Key Fields**:
  - `id`: UUID string for unique identification
  - `name`: Name of the household
  - `description`: Optional description of the household
  - `createdBy`: ID of the member who created the household
- **Database Table**: `households`
- **Relationships**:
  - Many-to-one with Member (as creator)
  - One-to-many with HouseholdMember
  - One-to-many with Task

#### HouseholdMember

- **Purpose**: Junction entity representing membership of a member in a household
- **Key Fields**:
  - `id`: UUID string for unique identification
  - `householdId`: Reference to the household
  - `memberId`: Reference to the member
  - `role`: Enum value (`admin` or `member`)
  - `isActive`: Boolean indicating if the member is active in the household
  - `invitedBy`: Optional ID of the member who sent the invitation
  - `joinedAt`: Optional timestamp when the member joined
- **Database Table**: `household_members`
- **Relationships**:
  - Many-to-one with Household
  - Many-to-one with Member
  - Many-to-one with Member (as inviter)

#### HouseholdMemberRole (Enum)

- **Values**: `admin`, `member`
- **Purpose**: Defines the role hierarchy within households

### Task Domain

#### Task

- **Purpose**: Represents a task that needs to be completed within a household
- **Key Fields**:
  - `id`: UUID string for unique identification
  - `title`: Title/name of the task
  - `description`: Detailed description of the task
  - `dueDate`: Optional timestamp when the task is due
  - `status`: Enum value indicating task status
  - `createdBy`: ID of the member who created the task
  - `householdId`: ID of the household the task belongs to
- **Database Table**: `tasks`
- **Relationships**:
  - Many-to-one with Member (as creator)
  - Many-to-one with Household

#### TaskStatus (Enum)

- **Values**: `pending`, `in_progress`, `completed`
- **Purpose**: Tracks the lifecycle of tasks

## Database Schema Overview

### Tables

1. **members**: Stores both AuthMember and Member data
2. **households**: Stores household information
3. **household_members**: Junction table for household membership
4. **tasks**: Stores task information

### Key Constraints

- All IDs are UUIDs (char(36))
- Foreign key relationships enforce referential integrity
- Unique constraints on email addresses
- Cascade deletes for household and task relationships

## Domain Separation

The system is organized into distinct domains:

1. **Auth Domain**: Handles user authentication and registration
2. **Member Domain**: Manages user profiles and information
3. **Household Domain**: Manages household creation and membership
4. **Task Domain**: Manages task creation and tracking

Each domain has its own:

- Controllers (HTTP endpoints)
- Handlers (business logic)
- Repositories (data access)
- Entities (data models)

## Data Flow Patterns

1. **Authentication Flow**: AuthMember → Member (same ID)
2. **Household Management**: Member → HouseholdMember → Household
3. **Task Management**: Member → Task → Household
4. **Authorization**: HouseholdMember validation for household access

## Validation Patterns

- All entities use UUID validation via `Guid.isValid()`
- Request validation follows the pattern: `fun validate(): List<String>`
- Validation errors are thrown as `ValidationException`
- Business rule validation (e.g., household membership) is handled by dedicated validators

## Repository Pattern

All data access goes through the Unit of Work pattern:

- `UnitOfWork`: Manages transactions
- `UnitOfWorkContext`: Provides repository access
- Repository interfaces define data access contracts
- JDBI implementations handle actual database operations
