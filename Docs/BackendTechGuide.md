# StreetMed Backend API Documentation - Updated January 2026

## 🌐 Production Endpoints

| Environment | Base URL |
|-------------|----------|
| **Production** | https://streetmed-backend-900663028964.us-central1.run.app |
| **Local Development** | http://localhost:8080 or https://localhost:8443 |
| **Swagger UI (Prod)** | https://streetmed-backend-900663028964.us-central1.run.app/swagger-ui.html |
| **Swagger UI (Local)** | http://localhost:8080/swagger-ui.html |

---

## Table of Contents
- [Technology Stack](#technology-stack)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Security Enhancements](#security-enhancements)
  - [ECDH Key Exchange](#ecdh-key-exchange)
  - [Encrypted Communication](#encrypted-communication)
  - [End-to-End Security Flow](#end-to-end-security-flow)
- [Authentication API](#authentication-api)
  - [Register](#register)
  - [Login](#login)
  - [Profile Management](#profile-management)
  - [Password Recovery](#password-recovery)
- [Email Service](#email-service)
  - [Email Configuration](#email-configuration)
- [Admin API](#admin-api)
  - [User Management](#user-management)
  - [Volunteer SubRole Management](#volunteer-subrole-management)
- [Order API](#order-api)
  - [Create Order](#create-order)
  - [Create Guest Order](#create-guest-order)
  - [View Orders](#view-orders)
  - [Update Order Status](#update-order-status)
  - [Cancel Order](#cancel-order)
  - [Volunteer Order Management](#volunteer-order-management)
- [Rounds API](#rounds-api)
  - [Admin Round Management](#admin-round-management)
  - [Volunteer Round Participation](#volunteer-round-participation)
- [Cargo Management API](#cargo-management-api)
  - [Add Item](#add-cargo-item)
  - [Update Item](#update-cargo-item)
  - [Get Items](#get-cargo-items)
  - [Low Stock Items](#get-low-stock-items)
- [Cargo Image API](#cargo-image-api)
  - [Upload Image](#upload-image)
  - [Get Image](#get-image)
  - [Delete Image](#delete-image)
- [Feedback API](#feedback-api)
  - [Submit Feedback](#submit-feedback)
  - [Get All Feedback](#get-all-feedback)
  - [Delete Feedback](#delete-feedback)
  - [Search Feedback](#search-feedback)
- [Volunteer API](#volunteer-api)
  - [Submit Application](#submit-volunteer-application)
  - [Get All Applications](#get-all-applications-admin-only)
  - [Get Pending Applications](#get-pending-applications-admin-only)
  - [Check Application Status](#check-volunteer-application-status)
  - [Approve Application](#approve-volunteer-application)
  - [Reject Application](#reject-volunteer-application)
- [Business Logic](#business-logic)
  - [Security Flow](#security-flow)
  - [Authentication Flow](#authentication-flow)
  - [Password Recovery Flow](#password-recovery-flow)
  - [Email Notification System](#email-notification-system)
  - [Order Flow](#order-flow)
  - [Round Management Flow](#round-management-flow)
  - [Order-Round Integration](#order-round-integration)
  - [Cargo Management Flow](#cargo-management-flow)
  - [Order-Inventory Integration](#order-inventory-integration)
  - [Feedback Management](#feedback-management)
  - [Volunteer Application Flow](#volunteer-application-flow)
  - [Access Control](#access-control)
  - [Volunteer SubRole System](#volunteer-subrole-system)

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.4.2 |
| Language | Java | 21 |
| Database | MySQL (Cloud SQL) | 8.0 |
| ORM | JPA/Hibernate | - |
| Email | Spring Mail | - |
| Build Tool | Maven | 3.6+ |
| Security | ECDH + AES-GCM | - |
| Password Hashing | BCrypt | - |
| Deployment | Google Cloud Run | - |
| CI/CD | GitHub Actions | - |

## Deployment

### Production Infrastructure

The application is deployed on **Google Cloud Platform**:

| Service | Platform | Details |
|---------|----------|---------|
| Backend | Cloud Run | `streetmed-backend` (us-central1) |
| Frontend | Cloud Run | `streetmed-frontend` (us-central1) |
| Database | Cloud SQL | MySQL 8.0 (`streetmedgo:us-central1:streetmed`) |
| Container Registry | Artifact Registry | `us-central1-docker.pkg.dev/streetmedgo/streetmedgo-repo` |

### CI/CD Pipeline

Automatic deployments via GitHub Actions:
- Push to `main` branch triggers deployment
- Backend: `.github/workflows/backenddeploy.yml`
- Frontend: `.github/workflows/frontenddeploy.yml`

### Environment Variables (Production)

```
MYSQL_DATABASE=streetmed
MYSQL_USER=streetmed
MYSQL_PASSWORD=<secret>
CORS_ALLOWED_ORIGINS=https://streetmed-frontend-900663028964.us-central1.run.app
CLIENT_AUTH_KEY=<secret>
MAIL_USERNAME=streetmedgo@gmail.com
MAIL_PASSWORD=<app-password>
```

---

## Project Structure
```
com.backend.streetmed_backend/
├── controller/
│   ├── Auth/
│   │   ├── AuthController.java
│   │   ├── AdminController.java
│   │   └── PasswordRecoveryController.java
│   ├── Inventory/
│   │   ├── CargoController.java
│   │   └── CargoImageController.java
│   ├── Order/
│   │   └── OrderController.java
│   ├── Rounds/
│   │   ├── AdminRoundsController.java
│   │   └── VolunteerRoundsController.java
│   ├── Security/
│   │   └── ECDHController.java 
│   └── Services/
│       ├── FeedbackController.java
│       └── VolunteerApplicationController.java
├── config/
│   ├── AsyncConfig.java
│   ├── CorsConfig.java
│   ├── DatabaseConfig.java
│   ├── MongoConfig.java
│   ├── MailConfig.java
│   └── WebMvcConfig.java
├── entity/
│   ├── user_entity/
│   │   ├── User.java
│   │   ├── UserMetadata.java
│   │   ├── VolunteerApplication.java
│   │   └── VolunteerSubRole.java
│   ├── order_entity/
│   │   ├── Order.java
│   │   └── OrderItem.java
│   ├── rounds_entity/
│   │   ├── Rounds.java
│   │   └── RoundSignup.java
│   ├── Service_entity/
│   │   └── Feedback.java
│   └── CargoItem.java
    |-- CargoImage.java
├
├── repository/
│   ├── Cargo/
│   │   ├── CargoItemRepository.java
│   │   └── CargoImageRepository.java
│   ├── Order/
│   │   ├── OrderRepository.java
│   │   └── OrderItemRepository.java
│   ├── Rounds/
│   │   ├── RoundsRepository.java
│   │   └── RoundSignupRepository.java
│   ├── User/
│   │   ├── UserRepository.java
│   │   ├── UserMetadataRepository.java
│   │   └── VolunteerSubRoleRepository.java
│   ├── FeedbackRepository.java
│   └── VolunteerApplicationRepository.java
├── service/
│   ├── UserService.java
│   ├── OrderService.java
│   ├── RoundsService.java
│   ├── RoundSignupService.java
│   ├── CargoItemService.java
│   ├── CargoImageService.java
│   ├── EmailService.java
│   ├── FeedbackService.java
│   ├── VolunteerApplicationService.java
│   ├── VolunteerSubRoleService.java
│   └── OrderRoundAssignmentService.java
├── security/
│   ├── ClientAuthenticationService.java
│   ├── ECDHService.java
│   ├── EncryptionUtil.java
│   ├── PasswordHash.java
│   ├── SecurityManager.java
│   └── TLSService.java
├── filter/
│   ├── OptionsRequestFilter.java
│   └── RequestCorsFilter.java
└── scheduler/
    └── OrderAssignmentScheduler.java
```

## Security Enhancements

The system incorporates enhanced security mechanisms to protect sensitive data during transmission between the client and server.

### ECDH Key Exchange

The application implements Elliptic Curve Diffie-Hellman (ECDH) key exchange protocol to establish a secure shared secret between client and server without transmitting the secret itself.

**Key Components:**
- `ECDHController`: Handles key exchange API endpoints
- `ECDHService`: Manages key pair generation and shared secret computation
- `SecurityManager`: Coordinates the security flow and session management

**Key Exchange Endpoints:**

```http
GET /api/security/initiate-handshake
Response:
{
    "sessionId": "string (UUID)",
    "serverPublicKey": "string (Base64-encoded public key)"
}
```

```http
POST /api/security/complete-handshake
Content-Type: application/json

Request Body:
{
    "sessionId": "string (UUID from initiate-handshake)",
    "clientPublicKey": "string (Base64-encoded public key)"
}

Response:
{
    "status": "success",
    "message": "Handshake completed successfully"
}
```

### Encrypted Communication

After key exchange, all sensitive API communications are encrypted using AES-GCM encryption:

**Key Components:**
- `EncryptionUtil`: Handles AES-GCM encryption/decryption operations
- `SecurityManager`: Manages session keys and encryption/decryption operations

**Process:**
1. The client includes the session ID in the X-Session-ID header
2. The request body is encrypted before transmission
3. The server uses the session ID to retrieve the correct encryption key
4. The server decrypts the request, processes it, and encrypts the response
5. The client decrypts the response using the shared key

### End-to-End Security Flow

1. **Session Initialization:**
   - Client requests a handshake and receives server public key and session ID
   - Client generates its own key pair and sends public key to server
   - Both sides independently compute the same shared secret
   - Shared secret is used to derive AES-256 symmetric key

2. **Secure Communication:**
   - All sensitive data is encrypted using AES-GCM with the derived key
   - Each request includes the session ID to identify the correct encryption key
   - Responses are encrypted using the same key

3. **Session Management:**
   - Sessions expire after 30 minutes of inactivity
   - Scheduled cleanup task removes expired sessions every 5 minutes
   - Each session has its own unique encryption key

---

## Authentication API

### Register
```http
POST /api/auth/register
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "username": "jsmith",
    "email": "jsmith@example.com",
    "password": "SecurePassword123",
    "phone": "412-555-0123" (optional)
}

Response:
{
    "status": "success",
    "message": "User registered successfully",
    "userId": 42
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "username": "jsmith@example.com",  // Can be username or email
    "password": "SecurePassword123"
}

Response:
{
    "status": "success",
    "message": "Login successful",
    "userId": 42,
    "role": "VOLUNTEER",
    "authenticated": true,
    "username": "jsmith",
    "email": "jsmith@example.com",
    "authToken": "uuid-token-string",
    "volunteerSubRole": "CLINICIAN"  // Only returned for VOLUNTEER role
}
```

### Profile Management

#### Update Username
```http
PUT /api/auth/update/username
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "userId": "42",
    "newUsername": "johnsmith",
    "authenticated": "true"
}

Response:
{
    "status": "success",
    "message": "Username updated successfully",
    "username": "johnsmith"
}
```

#### Update Password
```http
PUT /api/auth/update/password
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "userId": "42",
    "currentPassword": "SecurePassword123",
    "newPassword": "EvenMoreSecure456",
    "authenticated": "true"
}

Response:
{
    "status": "success",
    "message": "Password updated successfully"
}
```

#### Update Email
```http
PUT /api/auth/update/email
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "userId": "42",
    "currentPassword": "SecurePassword123",
    "newEmail": "john.smith@example.com",
    "authenticated": "true"
}

Response:
{
    "status": "success",
    "message": "Email updated successfully",
    "email": "john.smith@example.com"
}
```

#### Update Phone Number
```http
PUT /api/auth/update/phone
Content-Type: application/json
Headers:
  X-Session-ID: string (optional - for encrypted requests)

Request Body:
{
    "userId": "42",
    "currentPassword": "SecurePassword123",
    "newPhone": "412-555-9876",
    "authenticated": "true"
}

Response:
{
    "status": "success",
    "message": "Phone number updated successfully",
    "phone": "412-555-9876"
}
```

### Password Recovery

#### Request Password Reset
```http
POST /api/auth/password/request-reset
Content-Type: application/json

Request Body:
{
    "email": "jsmith@example.com"
}

Response:
{
    "status": "success",
    "message": "Recovery code sent to your email"
}
```

#### Verify OTP
```http
POST /api/auth/password/verify-otp
Content-Type: application/json

Request Body:
{
    "email": "jsmith@example.com",
    "otp": "123456"
}

Response:
{
    "status": "success",
    "message": "OTP verified successfully",
    "resetToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "userId": 42
}
```

#### Reset Password
```http
POST /api/auth/password/reset
Content-Type: application/json

Request Body:
{
    "resetToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
    "newPassword": "NewSecurePassword789"
}

Response:
{
    "status": "success",
    "message": "Password reset successfully"
}
```

---

## Email Service

The system includes a robust email notification service for various operations including password recovery, account creation, and volunteer application approvals.

### Email Configuration

Production configuration uses environment variables:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

The email service supports multiple types of notifications:
1. Password recovery emails with OTP codes (15-minute expiration)
2. New user credentials notifications
3. Volunteer application approval notifications
4. Round signup confirmations
5. Round cancellation notices
6. Lottery selection notifications
7. Reminders for upcoming rounds

---

## Admin API

### User Management

#### Get All Users
```http
GET /api/admin/users
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "authenticated": true,
    "data": {
        "clients": [...],
        "volunteers": [...],
        "admins": [...]
    }
}
```

#### Create User
```http
POST /api/admin/user/create
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "username": "newvolunteer",
    "email": "newvolunteer@example.com",
    "phone": "412-555-0456",
    "firstName": "New",
    "lastName": "Volunteer",
    "role": "VOLUNTEER"
}

Response:
{
    "status": "success",
    "message": "User created successfully",
    "userId": 43,
    "username": "newvolunteer",
    "role": "VOLUNTEER",
    "generatedPassword": "aP9!bZ7&cX5$"
}
```

---

## Order API

### Create Order
```http
POST /api/orders/create
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "userId": 42,
    "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
    "notes": "Please leave package at front door",
    "phoneNumber": "412-555-0123",
    "latitude": 40.4406,
    "longitude": -79.9959,
    "items": [
        {
            "itemName": "First Aid Kit",
            "quantity": 1
        },
        {
            "itemName": "Pain Reliever",
            "quantity": 2
        }
    ]
}

Response:
{
    "status": "success",
    "message": "Order created successfully",
    "orderId": 101
}
```

### Get All Orders (Admin/Volunteer)
```http
GET /api/orders/all?authenticated=true&userId=42&userRole=ADMIN

Response:
{
    "status": "success",
    "orders": [
        {
            "orderId": 101,
            "userId": 42,
            "status": "PENDING",
            "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
            "notes": "Please leave package at front door",
            "requestTime": "2026-01-28T15:30:20",
            "phoneNumber": "412-555-0123",
            "roundId": 5,
            "orderItems": [...]
        }
    ]
}
```

---

## Business Logic

### Order Flow

1. **Order Creation:**
   - Supports both authenticated and guest orders
   - Validates inventory availability before processing
   - Reserves inventory items by reducing quantities temporarily
   - Automatically assigns orders to upcoming rounds when possible
   - Initial status: PENDING

2. **Order Lifecycle:**
   ```
                      [inventory reserved]
                              |
   [Created] --------> PENDING --------> PROCESSING --------> COMPLETED
                         |                   |                 [inventory
                         |                   |              reduction permanent]
                         |                   |
                         +------------------+
                                 |
                                 v
                            CANCELLED
                       [inventory released]
   ```

3. **Order Management Permissions:**
   - CLIENT: Create orders, view own orders, cancel own orders
   - VOLUNTEER: View all orders, update order status, process orders
   - ADMIN: All operations plus system-wide management

### Access Control

1. **Authentication Methods (Production):**
   - Token-based: `X-Auth-Token` header
   - Header-based: `Authentication-Status: true`
   - Query parameter-based: `authenticated=true&userId=X&userRole=Y`

2. **Role-Based Access:**
   - CLIENT: Self-service operations, order creation/tracking
   - VOLUNTEER: Extended permissions for rounds and order processing
   - ADMIN: Full system access and management capabilities

---

*For the complete API documentation including all endpoints, see the full version in the repository.*

**Last Updated:** January 2026
**Deployment Platform:** Google Cloud Run
