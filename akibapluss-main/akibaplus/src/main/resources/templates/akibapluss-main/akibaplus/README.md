# Akibaplus Backend

## Overview

This is the backend for the Akibaplus SACCOS system, built with Spring Boot 3, Java 21, and PostgreSQL. It supports JWT-based authentication, role-based access control, and Flyway for database migrations.

## Prerequisites

- Java 21
- Maven
- PostgreSQL

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone <repository-url>
   ```

2. Navigate to the project directory:

   ```bash
   cd akibaplus-backend
   ```

3. Configure the database:
   - Update `src/main/resources/application.properties` with your PostgreSQL credentials.
4. Run Flyway migrations:

   ```bash
   mvn flyway:migrate
   ```

5. Build and run the application:

   ```bash
   mvn spring-boot:run
   ```

## API Endpoints

### Authentication

- `POST /api/auth/register`: Register a new user.
- `POST /api/auth/login`: Login and receive a JWT.

### Admin

- `POST /api/admin/members`: Add a new member.
- `POST /api/admin/transactions`: Record a transaction.

### Member

- `PUT /api/members/{id}`: Update member profile.

## Database Schema

Flyway manages the database schema. The initial schema is defined in `src/main/resources/db/migration/V1__initial_schema.sql`.

## Security

- JWT-based authentication.
- Role-based access control (Admin, Member).

## License

- MIT License.
