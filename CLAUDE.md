# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

This is a Spring Boot multi-module Maven project using Java 21. Common commands:

- **Build project**: `mvn clean install`
- **Build specific module**: `mvn clean install -pl edutest-domain`
- **Run web server**: `mvn spring-boot:run -pl edutest-web-server`
- **Run tests**: `mvn test`
- **Run specific module tests**: `mvn test -pl edutest-domain`

## Project Architecture

### Module Structure
- **edutest-parent**: Maven parent POM managing shared dependencies and configuration
- **edutest-domain**: Core domain models and business logic
- **edutest-commons**: Shared utilities, exceptions, security, validation
- **edutest-web-server**: REST API implementation and web layer
- **edutest-api-definition**: OpenAPI specification (edutest-api.yaml)

### Domain Architecture
The project uses Domain-Driven Design principles with a clear separation between domain models and persistence entities:

#### Domain Models (`com.edutest.domain`)
- **Assignment hierarchy**: Abstract `Assignment` base class with concrete implementations:
  - `CodingAssignment`: Programming assignments with test cases and execution limits
  - `MultipleChoiceAssignment`: Multiple choice questions with scoring analysis
  - `SingleChoiceAssignment`: Single choice questions
  - `OpenQuestionAssignment`: Free-form text questions
- **Test**: Container for assignments with timing, navigation, and randomization settings
- **TestAttempt**: Student's attempt at completing a test
- **User**: User accounts with role-based access (STUDENT, TEACHER, ADMIN)
- **StudentGroup**: Groups for organizing students and assigning tests

#### Persistence Layer (`com.edutest.persistance.entity`)
- Mirrors domain structure but optimized for JPA/database persistence
- All entities extend `BaseEntity` for common fields (id, createdAt, updatedAt, version)
- Uses JPA auditing with `@EntityListeners(AuditingEntityListener.class)`

### Key Design Patterns
- **Template Method**: Assignment abstract class defines common behavior while subclasses implement specific validation and scoring
- **Builder Pattern**: Used extensively with Lombok `@Builder` for object creation
- **Repository Pattern**: Implied in persistance package structure

### Technology Stack
- **Java 21** with Spring Boot 3.5.0
- **Spring Data JPA** for persistence
- **Spring Security** for authentication/authorization
- **PostgreSQL** database
- **Lombok** for reducing boilerplate
- **OpenAPI 3.0** for API specification

### API Structure
REST API follows OpenAPI specification in `edutest-api-definition/src/main/resources/api/edutest-api.yaml`:
- JWT-based authentication
- Role-based authorization (Student/Teacher/Admin)
- CRUD operations for users, groups, tests, and assignments
- Test execution and attempt management

## Important Conventions
- Use Lombok annotations consistently (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Domain models should be separate from persistence entities
- All entities must extend `BaseEntity`
- Follow the established package structure under `com.edutest`
- Use `LocalDateTime` for date/time fields
- Implement validation in domain models using abstract methods