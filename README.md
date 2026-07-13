# Academix

Spring Boot backend for signup and signin with MySQL and Argon2 password hashing.

## Requirements

- Java 21
- Maven 3.9+
- XAMPP with MySQL enabled

## Setup

1. Start MySQL from the XAMPP control panel.
2. Create a database named `academix` in phpMyAdmin or with SQL.
3. Review `.env` and update the database username/password if your XAMPP setup is different.
4. Run the API:

```bash
mvn spring-boot:run
```

## Auth endpoints

- `POST /api/auth/signup`
- `POST /api/auth/signin`

Example signup payload:

```json
{
	"fullName": "Alex Doe",
	"email": "alex@example.com",
	"password": "StrongPass123!",
	"role": 2
}
```

Role codes are `0` for `ADMINISTRATION`, `1` for `TEACHER`, and `2` for `STUDENT`.