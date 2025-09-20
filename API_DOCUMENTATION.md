# Doctoralia API Documentation

## Overview
This is the API documentation for the Doctoralia application - a medical appointment system.

**Base URL:** `http://localhost:8081`

## Authentication
This API uses JWT (JSON Web Token) for authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 🔐 Authentication Endpoints

### 1. User Registration
**Endpoint:** `POST /api/auth/register`

**Description:** Register a new user account (default role: PATIENT)

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- `email`: Required, valid email format
- `password`: Required, minimum 6 characters
- `firstName`: Required
- `lastName`: Required

**Success Response (200):**
```json
{
  "message": "User registered successfully!"
}
```

**Error Responses:**
```json
{
  "message": "Error: Email is already in use!"
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

### 2. User Login
**Endpoint:** `POST /api/auth/login`

**Description:** Authenticate user and receive JWT token

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "PATIENT"
}
```

**Error Response (400):**
```json
{
  "message": "Error: Invalid email or password!"
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

## 👤 User Management Endpoints

### 3. Get Current User Profile
**Endpoint:** `GET /api/users/me`

**Description:** Get current user's profile information

**Headers Required:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "role": "PATIENT",
  "phone": "123-456-7890",
  "createdAt": "2024-01-01T10:00:00"
}
```

**Error Response (400):**
```json
{
  "message": "Error: User not found!"
}
```

**Example using curl:**
```bash
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <your_jwt_token>"
```

---

### 4. Update User Profile
**Endpoint:** `PUT /api/users/me`

**Description:** Update current user's profile information

**Headers Required:**
```
Authorization: Bearer <jwt_token>
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "123-456-7890"
}
```

**Success Response (200):**
```json
{
  "message": "Profile updated successfully!"
}
```

**Error Response (400):**
```json
{
  "message": "Error: Invalid token!"
}
```

**Example using curl:**
```bash
curl -X PUT http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "phone": "123-456-7890"
  }'
```

---

## 🔑 User Roles

The system supports three user roles:

| Role | Description |
|------|-------------|
| `PATIENT` | Default role for new registrations |
| `DOCTOR` | Medical professionals |
| `ADMIN` | System administrators |

**Note:** New users can only register as `PATIENT`. Admin/Doctor roles must be assigned by existing administrators.

---

## 🗄️ Database Configuration

**Database:** PostgreSQL
- **Host:** localhost
- **Port:** 5433
- **Database:** doctorbook
- **Username:** admin
- **Password:** password

---

## 🔧 Development Setup

1. **Start PostgreSQL database** on port 5433
2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Application will start on:** http://localhost:8081

---

## 📝 Testing with Postman

### Import Collection
You can test these endpoints using Postman:

1. Create a new collection called "Doctoralia API"
2. Add the endpoints above
3. Set up environment variables:
   - `base_url`: http://localhost:8081
   - `jwt_token`: (set after login)

### Testing Flow
1. **Register** a new user
2. **Login** to get JWT token
3. **Set token** in Postman environment
4. **Test protected endpoints** (Get/Update profile)

---

## ⚠️ Security Notes

- All passwords are encrypted using BCrypt
- JWT tokens expire after 24 hours (86400000 ms)
- CORS is enabled for all origins (development only)
- Always use HTTPS in production
- Never commit JWT secrets to version control

---

## 🐛 Common Errors

| HTTP Code | Error Message | Solution |
|-----------|---------------|----------|
| 400 | "Email is already in use!" | Use a different email address |
| 400 | "Invalid email or password!" | Check credentials |
| 400 | "Invalid token!" | Re-login to get new token |
| 400 | "User not found!" | Token may be expired |

---

## 📚 Example API Testing Script

```javascript
// Postman Pre-request Script for auto-login
pm.sendRequest({
    url: pm.environment.get("base_url") + "/api/auth/login",
    method: 'POST',
    header: {
        'Content-Type': 'application/json',
    },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            "email": "test@example.com",
            "password": "password123"
        })
    }
}, function (err, response) {
    if (response.json().token) {
        pm.environment.set("jwt_token", response.json().token);
    }
});
```

---

## 📞 Support

For questions or issues, please contact the development team or create an issue in the project repository.