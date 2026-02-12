# ReadyKids Childminder Agency Portal (Java Spring Boot)

A full-stack registration and admin portal for a childminder agency, built with Spring Boot 3 and PostgreSQL.

## Tech Stack

- **Backend:** Java 17+, Spring Boot 3.2, Spring JDBC
- **Database:** PostgreSQL with JSONB columns for nested form data
- **Frontend:** Vanilla HTML/CSS/JS (no build step)

## Prerequisites

- **Java** 17+
- **Maven** 3.8+
- **PostgreSQL** 14+

## Setup

```bash
# 1. Create the PostgreSQL database
createdb readykids
```

Edit `src/main/resources/application.yml` with your PostgreSQL credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/readykids
    username: postgres
    password: yourpassword
```

## Building and Running

```bash
# Build
./mvnw clean package -DskipTests

# Run (schema and seed data are applied automatically on startup)
java -jar target/cma-1.0.0.jar
```

Or run directly with Maven:

```bash
./mvnw spring-boot:run
```

The server starts at **http://localhost:3000**.

## Pages

| URL | Description |
|-----|-------------|
| http://localhost:3000/register | 9-section childminder registration form |
| http://localhost:3000/admin | Admin dashboard with pipeline view and compliance tracking |

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/applications` | List all applications |
| GET | `/api/applications/{id}` | Get single application |
| POST | `/api/applications` | Submit new registration |
| PATCH | `/api/applications/{id}` | Update application fields |
| DELETE | `/api/applications/{id}` | Remove application |
| POST | `/api/applications/{id}/timeline` | Add audit log entry |

## Resetting the Database

```bash
dropdb readykids
createdb readykids
# Schema and seed data will be reapplied on next startup
./mvnw spring-boot:run
```
