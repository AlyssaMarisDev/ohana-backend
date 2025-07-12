## Technologies Used

### Core Technologies

#### Programming Language & Runtime

- **Kotlin** - Primary programming language
- **JVM** - Java Virtual Machine runtime

#### Web Framework

- **Ktor** - Kotlin web framework
  - Ktor Server Core
  - Ktor Server Netty (HTTP engine)
  - Ktor Server CORS
  - Ktor Server Status Pages
  - Ktor Server Content Negotiation
  - Ktor Serialization Jackson

#### Dependency Injection

- **Koin** - Lightweight dependency injection framework for Kotlin

#### Database & ORM

- **JDBI** - SQL convenience library for Java
  - JDBI Core
  - JDBI Kotlin
  - JDBI SQLObject
  - JDBI Kotlin SQLObject
- **MySQL** (v9.0.0) - Primary database

#### Serialization

- **Jackson** (v2.15.2) - JSON processing library
  - Jackson Databind
  - Jackson Module Kotlin
  - Jackson Datatype JSR310 (Java 8 Date/Time support)

#### Logging

- **Logback** (v1.4.14) - Logging framework
- **SLF4J** - Logging facade (via Logback)

### Build & Development Tools

#### Build System

- **Gradle** (v8.10.2) - Build automation tool
- **Gradle Kotlin DSL** - Build scripts written in Kotlin

#### Code Quality

- **ktlint** (v1.5.0) - Kotlin linter
- **Gradle ktlint Plugin** (v12.2.0) - Integration with build system

#### Testing

- **JUnit 5**
- **H2 Database** (v1.4.200) - In-memory database for testing

#### Development Features

- **Hot Reload** - Development mode with automatic reloading
- **Fat JAR** - Executable JAR with all dependencies included
