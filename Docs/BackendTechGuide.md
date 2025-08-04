# StreetMed Backend API Documentation - Updated April 2025

## Table of Contents
- [Technology Stack](#technology-stack)
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
- [Deployment](#deployment)

## Technology Stack
- Spring Boot 3.4.2
- Java 17
- MySQL Database (with Google Cloud SQL integration)
- MongoDB (for image storage)
- JPA/Hibernate
- Spring Mail (for email notifications)
- Maven
- Cross-Origin Resource Sharing (CORS) enabled for specified origins
- Spring Security Crypto (for password hashing)
- AES-GCM encryption for secure communications
- ECDH key exchange using NIST P-256 curve
- Asynchronous processing with dedicated thread pools
- Google App Engine for deployment

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
├── document/
│   └── CargoImage.java
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
│   └── SecurityManager.java
├── filter/
│   ├── OptionsRequestFilter.java
│   └── RequestCorsFilter.java
└── scheduler/
    └── OrderAssignmentScheduler.java
```

## Security Enhancements

The system now incorporates enhanced security mechanisms to protect sensitive data during transmission between the client and server.

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

After key exchange, all sensitive API communications can be encrypted using AES-GCM encryption:

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

## Email Service

The system includes a robust email notification service for various operations including password recovery, account creation, and volunteer application approvals.

### Email Configuration

Configuration in application.properties:
```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=streetmedgo@gmail.com
spring.mail.password=zmcjlhadinbhddfe
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
        "clients": [
            {
                "userId": 1,
                "username": "client1",
                "email": "client1@example.com",
                "phone": "412-555-0001",
                "role": "CLIENT"
            }
        ],
        "volunteers": [
            {
                "userId": 2,
                "username": "volunteer1",
                "email": "volunteer1@example.com",
                "phone": "412-555-0002",
                "role": "VOLUNTEER",
                "volunteerSubRole": "CLINICIAN"
            }
        ],
        "admins": [
            {
                "userId": 3,
                "username": "admin",
                "email": "admin@example.com",
                "phone": "412-555-0003",
                "role": "ADMIN"
            }
        ]
    }
}
```

#### Get User Details
```http
GET /api/admin/user/42
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "data": {
        "userId": 42,
        "username": "jsmith",
        "email": "jsmith@example.com",
        "phone": "412-555-0123",
        "role": "VOLUNTEER",
        "firstName": "John",
        "lastName": "Smith",
        "createdAt": "2025-01-15T10:30:00",
        "lastLogin": "2025-04-16T14:25:10"
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

#### Update User Information
```http
PUT /api/admin/user/update/42
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "username": "johnsmith",
    "email": "john.smith@example.com",
    "phone": "412-555-9876",
    "role": "VOLUNTEER",
    "firstName": "John",
    "lastName": "Smith"
}

Response:
{
    "status": "success",
    "message": "User updated successfully",
    "userId": 42,
    "currentUsername": "jsmith",
    "updatedFields": {
        "username": "johnsmith",
        "email": "john.smith@example.com",
        "phone": "412-555-9876"
    }
}
```

#### Delete User
```http
DELETE /api/admin/user/delete
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "authenticated": "true",
    "adminUsername": "admin",
    "username": "johnsmith"
}

Response:
{
    "status": "success",
    "message": "User deleted successfully",
    "authenticated": true
}
```

#### Reset User Password
```http
PUT /api/admin/user/reset-password/42
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "newPassword": "ResetPassword789"
}

Response:
{
    "status": "success",
    "message": "Password reset successfully",
    "userId": 42,
    "username": "johnsmith"
}
```

### Volunteer SubRole Management

#### Update Volunteer SubRole
```http
PUT /api/admin/volunteer/subrole
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "userId": 42,
    "volunteerSubRole": "CLINICIAN",
    "notes": "Approved as clinician based on medical background"
}

Response:
{
    "status": "success",
    "message": "Volunteer sub role updated successfully",
    "volunteerSubRole": "CLINICIAN"
}
```

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
    "orderId": 101,
    "authenticated": true
}
```

### Create Guest Order
```http
POST /api/orders/guest/create
Content-Type: application/json

Request Body:
{
    "firstName": "Guest",
    "lastName": "User",
    "email": "guest@example.com",
    "phone": "412-555-7890",
    "deliveryAddress": "456 Oak St, Pittsburgh, PA 15213",
    "notes": "Need medical supplies urgently",
    "items": [
        {
            "itemName": "Bandages",
            "quantity": 3
        },
        {
            "itemName": "Antiseptic",
            "quantity": 1
        }
    ]
}

Response:
{
    "status": "success",
    "message": "Guest order created successfully",
    "orderId": 102,
    "orderStatus": "PENDING"
}
```

### View Orders

#### Get All Orders (Volunteer Only)
```http
GET /api/orders/all?authenticated=true&userId=42&userRole=VOLUNTEER

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
            "requestTime": "2025-04-16T15:30:20",
            "phoneNumber": "412-555-0123",
            "roundId": 5,
            "orderItems": [
                {
                    "itemName": "First Aid Kit",
                    "quantity": 1
                },
                {
                    "itemName": "Pain Reliever",
                    "quantity": 2
                }
            ]
        },
        {
            "orderId": 102,
            "userId": -1,
            "orderType": "GUEST",
            "status": "PENDING",
            "deliveryAddress": "456 Oak St, Pittsburgh, PA 15213",
            "notes": "Need medical supplies urgently",
            "requestTime": "2025-04-16T16:45:10",
            "phoneNumber": "412-555-7890",
            "roundId": 5,
            "orderItems": [
                {
                    "itemName": "Bandages",
                    "quantity": 3
                },
                {
                    "itemName": "Antiseptic",
                    "quantity": 1
                }
            ]
        }
    ],
    "authenticated": true
}
```

#### Get User Orders
```http
GET /api/orders/user/42?authenticated=true&userRole=CLIENT&userId=42

Response:
{
    "status": "success",
    "orders": [
        {
            "orderId": 101,
            "status": "PENDING",
            "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
            "notes": "Please leave package at front door",
            "requestTime": "2025-04-16T15:30:20",
            "phoneNumber": "412-555-0123",
            "roundId": 5,
            "orderItems": [
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
    ],
    "authenticated": true
}
```

### Update Order Status
```http
PUT /api/orders/101/status
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "userId": 42,
    "userRole": "VOLUNTEER",
    "status": "PROCESSING"
}

Response:
{
    "status": "success",
    "message": "Order status updated successfully",
    "orderStatus": "PROCESSING",
    "authenticated": true
}
```

### Cancel Order
```http
POST /api/orders/101/cancel
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "userId": 42,
    "userRole": "CLIENT"
}

Response:
{
    "status": "success",
    "message": "Order cancelled successfully",
    "authenticated": true
}
```

### Volunteer Order Management

#### Get Volunteer Assigned Orders
```http
GET /api/orders/volunteer/assigned?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "orders": [
        {
            "orderId": 101,
            "userId": 42,
            "status": "PROCESSING",
            "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
            "notes": "Please leave package at front door",
            "requestTime": "2025-04-16T15:30:20",
            "volunteerId": 42,
            "orderItems": [
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
    ],
    "authenticated": true
}
```

#### Get Volunteer Active Orders
```http
GET /api/orders/volunteer/assigned/active?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "orders": [
        {
            "orderId": 101,
            "userId": 42,
            "status": "PROCESSING",
            "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
            "notes": "Please leave package at front door",
            "requestTime": "2025-04-16T15:30:20",
            "volunteerId": 42,
            "orderItems": [
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
    ],
    "orderCount": 1,
    "authenticated": true
}
```

## Rounds API

### Admin Round Management

#### Create Round
```http
POST /api/admin/rounds/create
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "adminUsername": "admin",
    "title": "Downtown Outreach",
    "description": "Medical outreach in downtown area",
    "startTime": "2025-04-20T18:00:00",
    "endTime": "2025-04-20T21:00:00",
    "location": "Market Square, Downtown Pittsburgh",
    "maxParticipants": 5
}

Response:
{
    "status": "success",
    "message": "Round created successfully",
    "roundId": 5
}
```

#### Update Round
```http
PUT /api/admin/rounds/5
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "adminUsername": "admin",
    "title": "Updated Downtown Outreach",
    "description": "Updated medical outreach in downtown area",
    "startTime": "2025-04-20T19:00:00",
    "endTime": "2025-04-20T22:00:00",
    "location": "Market Square, Downtown Pittsburgh",
    "maxParticipants": 6,
    "status": "SCHEDULED"
}

Response:
{
    "status": "success",
    "message": "Round updated successfully",
    "roundId": 5
}
```

#### Cancel Round
```http
PUT /api/admin/rounds/5/cancel
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "adminUsername": "admin"
}

Response:
{
    "status": "success",
    "message": "Round cancelled successfully",
    "roundId": 5
}
```

#### Get All Rounds
```http
GET /api/admin/rounds/all?authenticated=true&adminUsername=admin

Response:
{
    "status": "success",
    "rounds": [
        {
            "roundId": 5,
            "title": "Downtown Outreach",
            "description": "Medical outreach in downtown area",
            "startTime": "2025-04-20T18:00:00",
            "endTime": "2025-04-20T21:00:00",
            "location": "Market Square, Downtown Pittsburgh",
            "maxParticipants": 5,
            "status": "SCHEDULED",
            "createdAt": "2025-04-15T10:30:00",
            "updatedAt": "2025-04-15T10:30:00"
        }
    ]
}
```

#### Get Round Details with Participants
```http
GET /api/admin/rounds/5?authenticated=true&adminUsername=admin

Response:
{
    "status": "success",
    "round": {
        "roundId": 5,
        "title": "Downtown Outreach",
        "description": "Medical outreach in downtown area",
        "startTime": "2025-04-20T18:00:00",
        "endTime": "2025-04-20T21:00:00",
        "location": "Market Square, Downtown Pittsburgh",
        "status": "SCHEDULED",
        "maxParticipants": 5,
        "confirmedVolunteers": 2,
        "availableSlots": 3,
        "openForSignup": true,
        "hasTeamLead": true,
        "hasClinician": true,
        "teamLead": {
            "userId": 43,
            "username": "teamlead1",
            "email": "teamlead1@example.com",
            "phone": "412-555-1111",
            "firstName": "Team",
            "lastName": "Lead"
        },
        "clinician": {
            "userId": 42,
            "username": "johnsmith",
            "email": "john.smith@example.com",
            "phone": "412-555-9876",
            "firstName": "John",
            "lastName": "Smith"
        }
    },
    "signups": [
        {
            "signupId": 10,
            "userId": 42,
            "status": "CONFIRMED",
            "role": "CLINICIAN",
            "signupTime": "2025-04-16T09:30:00",
            "username": "johnsmith",
            "email": "john.smith@example.com",
            "phone": "412-555-9876",
            "firstName": "John",
            "lastName": "Smith"
        },
        {
            "signupId": 11,
            "userId": 43,
            "status": "CONFIRMED",
            "role": "TEAM_LEAD",
            "signupTime": "2025-04-16T10:15:00",
            "username": "teamlead1",
            "email": "teamlead1@example.com",
            "phone": "412-555-1111",
            "firstName": "Team",
            "lastName": "Lead"
        },
        {
            "signupId": 12,
            "userId": 44,
            "status": "WAITLISTED",
            "role": "VOLUNTEER",
            "signupTime": "2025-04-16T11:20:00",
            "lotteryNumber": 1234,
            "username": "volunteer1",
            "email": "volunteer1@example.com",
            "phone": "412-555-2222",
            "firstName": "Regular",
            "lastName": "Volunteer"
        }
    ],
    "participantCounts": {
        "maxParticipants": 5,
        "confirmedVolunteers": 2,
        "availableSlots": 3,
        "waitlistedCount": 1,
        "hasTeamLead": true,
        "hasClinician": true
    }
}
```

#### Run Lottery for a Round
```http
POST /api/admin/rounds/5/lottery
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "adminUsername": "admin"
}

Response:
{
    "status": "success",
    "message": "Lottery run successfully",
    "selectedVolunteers": 2
}
```

### Volunteer Round Participation

#### Get All Upcoming Rounds
```http
GET /api/rounds/all?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "rounds": [
        {
            "roundId": 5,
            "title": "Downtown Outreach",
            "description": "Medical outreach in downtown area",
            "startTime": "2025-04-20T18:00:00",
            "endTime": "2025-04-20T21:00:00",
            "location": "Market Square, Downtown Pittsburgh",
            "status": "SCHEDULED",
            "totalSlots": 5,
            "confirmedVolunteers": 2,
            "availableSlots": 3,
            "openForSignup": true,
            "userSignedUp": true,
            "isTeamLead": false,
            "isClinician": true
        },
        {
            "roundId": 6,
            "title": "Northside Outreach",
            "description": "Medical outreach in northside area",
            "startTime": "2025-04-25T18:00:00",
            "endTime": "2025-04-25T21:00:00",
            "location": "Allegheny Commons, Northside",
            "status": "SCHEDULED",
            "totalSlots": 4,
            "confirmedVolunteers": 0,
            "availableSlots": 4,
            "openForSignup": true,
            "userSignedUp": false,
            "isTeamLead": false,
            "isClinician": false
        }
    ],
    "authenticated": true
}
```

#### Get Round Details
```http
GET /api/rounds/5?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "round": {
        "roundId": 5,
        "title": "Downtown Outreach",
        "description": "Medical outreach in downtown area",
        "startTime": "2025-04-20T18:00:00",
        "endTime": "2025-04-20T21:00:00",
        "location": "Market Square, Downtown Pittsburgh",
        "status": "SCHEDULED",
        "maxParticipants": 5,
        "confirmedVolunteers": 2,
        "availableSlots": 3,
        "openForSignup": true,
        "userSignedUp": true,
        "isTeamLead": false,
        "isClinician": true,
        "hasTeamLead": true,
        "hasClinician": true,
        "signupDetails": {
            "signupId": 10,
            "role": "CLINICIAN",
            "status": "CONFIRMED",
            "signupTime": "2025-04-16T09:30:00"
        }
    },
    "authenticated": true
}
```

#### Get My Rounds
```http
GET /api/rounds/my-rounds?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "upcomingRounds": [
        {
            "roundId": 5,
            "title": "Downtown Outreach",
            "description": "Medical outreach in downtown area",
            "startTime": "2025-04-20T18:00:00",
            "endTime": "2025-04-20T21:00:00",
            "location": "Market Square, Downtown Pittsburgh",
            "status": "SCHEDULED",
            "signupId": 10,
            "role": "CLINICIAN",
            "signupStatus": "CONFIRMED",
            "signupTime": "2025-04-16T09:30:00",
            "isPast": false,
            "canCancel": true,
            "isTeamLead": false,
            "isClinician": true
        }
    ],
    "pastRounds": [
        {
            "roundId": 4,
            "title": "Oakland Outreach",
            "description": "Medical outreach in Oakland area",
            "startTime": "2025-04-10T18:00:00",
            "endTime": "2025-04-10T21:00:00",
            "location": "Schenley Plaza, Oakland",
            "status": "COMPLETED",
            "signupId": 9,
            "role": "CLINICIAN",
            "signupStatus": "CONFIRMED",
            "signupTime": "2025-04-05T09:30:00",
            "isPast": true,
            "canCancel": false,
            "isTeamLead": false,
            "isClinician": true
        }
    ],
    "authenticated": true
}
```

#### Signup for Round
```http
POST /api/rounds/6/signup
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "userId": 42,
    "userRole": "VOLUNTEER",
    "requestedRole": "CLINICIAN"
}

Response:
{
    "status": "success",
    "message": "You have been confirmed as CLINICIAN",
    "signupId": 13,
    "signupStatus": "CONFIRMED",
    "authenticated": true
}
```

#### Cancel Signup
```http
DELETE /api/rounds/signup/13
Content-Type: application/json

Request Body:
{
    "authenticated": true,
    "userId": 42,
    "userRole": "VOLUNTEER"
}

Response:
{
    "status": "success",
    "message": "Signup cancelled successfully",
    "authenticated": true
}
```

#### Get Round Orders
```http
GET /api/rounds/5/orders?authenticated=true&userId=42&userRole=VOLUNTEER

Response:
{
    "status": "success",
    "orders": [
        {
            "orderId": 101,
            "userId": 42,
            "status": "PROCESSING",
            "deliveryAddress": "123 Main St, Pittsburgh, PA 15213",
            "notes": "Please leave package at front door",
            "requestTime": "2025-04-16T15:30:20",
            "volunteerId": 42,
            "phoneNumber": "412-555-0123",
            "orderItems": [
                {
                    "itemName": "First Aid Kit",
                    "quantity": 1
                },
                {
                    "itemName": "Pain Reliever",
                    "quantity": 2
                }
            ]
        },
        {
            "orderId": 102,
            "userId": -1,
            "orderType": "GUEST",
            "status": "PENDING",
            "deliveryAddress": "456 Oak St, Pittsburgh, PA 15213",
            "notes": "Need medical supplies urgently",
            "requestTime": "2025-04-16T16:45:10",
            "phoneNumber": "412-555-7890",
            "orderItems": [
                {
                    "itemName": "Bandages",
                    "quantity": 3
                },
                {
                    "itemName": "Antiseptic",
                    "quantity": 1
                }
            ]
        }
    ],
    "authenticated": true
}
```

## Cargo Management API

### Add Cargo Item
```http
POST /api/cargo/items
Content-Type: multipart/form-data
Headers:
  Admin-Username: admin
  Authentication-Status: true

Form Data:
  data: {
    "name": "Antibiotic Ointment",
    "quantity": 100,
    "description": "Topical antibiotic ointment for minor cuts and abrasions",
    "category": "First Aid",
    "minQuantity": 20,
    "sizeQuantities": {}
  }
  image: [binary file data]

Response:
{
    "status": "success",
    "message": "Item added successfully",
    "itemId": 50
}
```

### Update Cargo Item
```http
PUT /api/cargo/items/50
Content-Type: application/json
Headers:
  Admin-Username: admin
  Authentication-Status: true

Request Body:
{
    "name": "Antibiotic Ointment",
    "description": "Topical antibiotic ointment for minor cuts and abrasions",
    "category": "First Aid",
    "quantity": 80,
    "minQuantity": 15,
    "isAvailable": true,
    "needsPrescription": false
}

Response:
{
    "status": "success",
    "message": "Item updated successfully",
    "item": {
        "id": 50,
        "name": "Antibiotic Ointment",
        "description": "Topical antibiotic ointment for minor cuts and abrasions",
        "category": "First Aid",
        "quantity": 80,
        "minQuantity": 15,
        "isAvailable": true,
        "needsPrescription": false,
        "createdAt": "2025-04-15T14:30:00",
        "updatedAt": "2025-04-17T10:25:30"
    }
}
```

### Get Cargo Items
```http
GET /api/cargo/items

Response:
[
    {
        "id": 49,
        "name": "First Aid Kit",
        "description": "Basic first aid kit with bandages, gauze, and antiseptic wipes",
        "category": "First Aid",
        "quantity": 45,
        "sizeQuantities": {},
        "imageId": "6078a1b2c3d4e5f6a7b8c9d0",
        "isAvailable": true,
        "minQuantity": 10,
        "needsPrescription": false,
        "createdAt": "2025-04-10T11:20:00",
        "updatedAt": "2025-04-16T09:15:10"
    },
    {
        "id": 50,
        "name": "Antibiotic Ointment",
        "description": "Topical antibiotic ointment for minor cuts and abrasions",
        "category": "First Aid",
        "quantity": 80,
        "sizeQuantities": {},
        "imageId": "6078a1b2c3d4e5f6a7b8c9d1",
        "isAvailable": true,
        "minQuantity": 15,
        "needsPrescription": false,
        "createdAt": "2025-04-15T14:30:00",
        "updatedAt": "2025-04-17T10:25:30"
    },
    {
        "id": 51,
        "name": "Cotton T-Shirt",
        "description": "Basic cotton t-shirt",
        "category": "Clothing",
        "quantity": 0,
        "sizeQuantities": {
            "S": 10,
            "M": 15,
            "L": 12,
            "XL": 8
        },
        "imageId": "6078a1b2c3d4e5f6a7b8c9d2",
        "isAvailable": true,
        "minQuantity": 5,
        "needsPrescription": false,
        "createdAt": "2025-04-16T13:40:20",
        "updatedAt": "2025-04-16T13:40:20"
    }
]
```

### Get Low Stock Items
```http
GET /api/cargo/items/low-stock
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
[
    {
        "id": 49,
        "name": "First Aid Kit",
        "description": "Basic first aid kit with bandages, gauze, and antiseptic wipes",
        "category": "First Aid",
        "quantity": 9,
        "sizeQuantities": {},
        "imageId": "6078a1b2c3d4e5f6a7b8c9d0",
        "isAvailable": true,
        "minQuantity": 10,
        "needsPrescription": false,
        "createdAt": "2025-04-10T11:20:00",
        "updatedAt": "2025-04-16T09:15:10"
    }
]
```

## Cargo Image API

### Upload Image
```http
POST /api/cargo/images/upload
Content-Type: multipart/form-data
Headers:
  Authentication-Status: true

Form Data:
  file: [binary image data]
  cargoItemId: 50 (optional)

Response:
{
    "status": "success",
    "imageId": "6078a1b2c3d4e5f6a7b8c9d1",
    "message": "Image uploaded successfully"
}
```

### Get Image
```http
GET /api/cargo/images/6078a1b2c3d4e5f6a7b8c9d1

Response:
  Binary image data with appropriate content-type
```

### Delete Image
```http
DELETE /api/cargo/images/6078a1b2c3d4e5f6a7b8c9d1
Headers:
  Authentication-Status: true

Response:
{
    "status": "success",
    "message": "Image deleted successfully"
}
```

## Feedback API

### Submit Feedback
```http
POST /api/feedback/submit
Content-Type: application/json

Request Body:
{
    "name": "John Smith",
    "phoneNumber": "412-555-0123",
    "content": "The service was excellent! Very responsive team and the medical supplies were delivered promptly."
}

Response:
{
    "status": "success",
    "message": "Feedback submitted successfully",
    "feedbackId": 25
}
```

### Get All Feedback
```http
GET /api/feedback/all
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "data": [
        {
            "id": 25,
            "name": "John Smith",
            "phoneNumber": "412-555-0123",
            "content": "The service was excellent! Very responsive team and the medical supplies were delivered promptly.",
            "createdAt": "2025-04-17T11:30:45"
        },
        {
            "id": 24,
            "name": "Jane Doe",
            "phoneNumber": "412-555-4567",
            "content": "Great experience overall. Would recommend to others in need of medical supplies.",
            "createdAt": "2025-04-15T16:20:10"
        }
    ]
}
```

### Delete Feedback
```http
DELETE /api/feedback/25
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "message": "Feedback deleted successfully"
}
```

### Search Feedback
```http
GET /api/feedback/search?name=John
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "data": [
        {
            "id": 25,
            "name": "John Smith",
            "phoneNumber": "412-555-0123",
            "content": "The service was excellent! Very responsive team and the medical supplies were delivered promptly.",
            "createdAt": "2025-04-17T11:30:45"
        }
    ]
}
```

## Volunteer API

### Submit Volunteer Application
```http
POST /api/volunteer/apply
Content-Type: application/json

Request Body:
{
    "firstName": "Sarah",
    "lastName": "Johnson",
    "email": "sarah.johnson@example.com",
    "phone": "412-555-9876",
    "notes": "I have 5 years of experience as a licensed practical nurse and would like to contribute my skills to the community."
}

Response:
{
    "status": "success",
    "message": "Application submitted successfully",
    "applicationId": 15
}
```

### Get All Applications (Admin Only)
```http
GET /api/volunteer/applications
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "data": {
        "pending": [
            {
                "applicationId": 15,
                "firstName": "Sarah",
                "lastName": "Johnson",
                "email": "sarah.johnson@example.com",
                "phone": "412-555-9876",
                "status": "PENDING",
                "notes": "I have 5 years of experience as a licensed practical nurse and would like to contribute my skills to the community.",
                "submissionDate": "2025-04-17T09:45:30"
            }
        ],
        "approved": [
            {
                "applicationId": 14,
                "firstName": "John",
                "lastName": "Smith",
                "email": "john.smith@example.com",
                "phone": "412-555-9876",
                "status": "APPROVED",
                "notes": "Medical background with experience in community outreach.",
                "submissionDate": "2025-04-10T14:20:15"
            }
        ],
        "rejected": [
            {
                "applicationId": 13,
                "firstName": "Test",
                "lastName": "User",
                "email": "test@example.com",
                "phone": "412-555-1111",
                "status": "REJECTED",
                "notes": "This is a test application.",
                "submissionDate": "2025-04-05T10:30:00"
            }
        ]
    }
}
```

### Get Pending Applications (Admin Only)
```http
GET /api/volunteer/pending
Headers:
  Admin-Username: admin
  Authentication-Status: true

Response:
{
    "status": "success",
    "data": [
        {
            "applicationId": 15,
            "firstName": "Sarah",
            "lastName": "Johnson",
            "email": "sarah.johnson@example.com",
            "phone": "412-555-9876",
            "notes": "I have 5 years of experience as a licensed practical nurse and would like to contribute my skills to the community.",
            "submissionDate": "2025-04-17T09:45:30"
        }
    ]
}
```

### Check Volunteer Application Status
```http
GET /api/volunteer/application/status/sarah.johnson@example.com

Response:
{
    "status": "success",
    "applicationId": 15,
    "firstName": "Sarah",
    "lastName": "Johnson",
    "email": "sarah.johnson@example.com",
    "phone": "412-555-9876",
    "applicationStatus": "PENDING",
    "notes": "I have 5 years of experience as a licensed practical nurse and would like to contribute my skills to the community.",
    "submissionDate": "2025-04-17T09:45:30"
}
```

### Approve Volunteer Application
```http
POST /api/volunteer/approve
Content-Type: application/json

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "applicationId": "15"
}

Response:
{
    "status": "success",
    "message": "Application approved and volunteer account created",
    "applicationId": 15,
    "userId": 45,
    "initialPassword": "streetmed@pitt"
}
```

### Reject Volunteer Application
```http
POST /api/volunteer/reject
Content-Type: application/json

Request Body:
{
    "adminUsername": "admin",
    "authenticated": "true",
    "applicationId": "16"
}

Response:
{
    "status": "success",
    "message": "Application rejected",
    "applicationId": 16
}
```

## Business Logic

### Security Flow

1. **ECDH Key Exchange:**
   - Client initiates a request to the `/api/security/initiate-handshake` endpoint
   - Server generates a key pair and returns the public key along with a session ID
   - Client generates its own key pair and sends the public key to the server via `/api/security/complete-handshake`
   - Both sides compute the same shared secret independently
   - The shared secret is hashed with SHA-256 to derive an AES-256 symmetric key
   - The derived key is used for encryption/decryption of subsequent communications

2. **Request Processing with Encryption:**
   - Controllers parse requests flexibly:
     - If X-Session-ID header is present, attempt to decrypt the body
     - If decryption fails or no session ID, treat as regular JSON
   - Controllers build responses with similar flexibility:
     - If session ID and valid key exist, encrypt the response
     - Otherwise return regular JSON response

3. **Session Management:**
   - Session data includes key pairs, computed shared secrets, and derived AES keys
   - Sessions expire after 30 minutes of inactivity
   - Scheduled cleanup task runs every 5 minutes to remove expired sessions
   - Each session has a unique ID (UUID) and encryption key

### Authentication Flow

1. **Registration Process:**
   - Users can register with username, email, and password
   - System validates username and email uniqueness
   - Password is securely hashed using BCrypt before storage
   - System creates user metadata automatically
   - Default role assignment is CLIENT

2. **Login Process:**
   - Validates username/email and password against stored records
   - Verifies password hash using BCrypt
   - Updates last login timestamp
   - Returns user role, authentication status, and profile information
   - For volunteers, includes their specialized sub-role (CLINICIAN, TEAM_LEAD, or REGULAR)

3. **Profile Management:**
   - Users can update their username, email, phone, and password
   - Password changes require current password verification
   - Email changes require current password verification
   - All updates check for conflicts with existing users

4. **Admin Access:**
   - Protected admin endpoints with role verification via headers
   - User management capabilities (create, update, delete)
   - Password reset functionality with direct password setting
   - Volunteer sub-role management

### Password Recovery Flow

1. **Password Reset Request:**
   - User requests password reset by providing email
   - System generates a 6-digit OTP code
   - OTP is stored in memory with 15-minute expiration
   - System sends OTP to user's email address

2. **OTP Verification:**
   - User submits OTP code for verification
   - System verifies OTP code validity and expiration
   - If valid, system generates a reset token
   - Reset token is stored with the user ID

3. **Password Reset:**
   - User submits reset token and new password
   - System verifies token validity
   - Password is updated and token is removed
   - User can now log in with the new password

### Email Notification System

1. **System Configuration:**
   - SMTP settings in application.properties
   - Dedicated thread pool for email operations (3-6 threads)
   - Timeouts to prevent hanging operations

2. **Notification Types:**
   - Password recovery emails with OTP codes
   - New user account creation notification with credentials
   - Volunteer application approval notification with login info
   - Round signup confirmations and waitlist notifications
   - Round cancellation notices
   - Lottery selection notifications
   - Reminders for upcoming rounds (24 hours before)

3. **Performance Considerations:**
   - Non-blocking asynchronous processing
   - Email failures don't affect main application flow
   - Graceful error handling and logging
   - Service can be enabled/disabled globally

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

4. **Round Assignment:**
   - Orders are automatically assigned to rounds based on scheduling
   - Assignment considers volunteer capacity and load balancing
   - Orders can be manually reassigned if needed

### Round Management Flow

1. **Round Creation:**
   - Admin creates round with title, location, date/time, max participants
   - System initializes round with SCHEDULED status
   - Round details can be updated before execution

2. **Volunteer Signup Process:**
   - Volunteers can sign up with specific roles (VOLUNTEER, CLINICIAN, TEAM_LEAD)
   - CLINICIAN and TEAM_LEAD roles are auto-confirmed if positions are available
   - Regular volunteers are added to waitlist with lottery number
   - Admin can run lottery to fill available slots based on lottery numbers
   - Volunteers can cancel signup up to 24 hours before round

3. **Round Execution:**
   - Rounds progress from SCHEDULED to COMPLETED or CANCELED
   - Admin can cancel rounds with automatic notification to all participants
   - System tracks participant attendance and order fulfillment
   - Email reminders sent 24 hours before round start

### Order-Round Integration

1. **Automatic Assignment:**
   - `OrderRoundAssignmentService` assigns orders to upcoming rounds
   - Assignment runs on order creation and periodically via scheduler
   - System balances order load based on volunteer count
   - Maximum ratio of 5 orders per volunteer

2. **Rebalancing:**
   - When volunteers cancel, system rebalances assigned orders
   - When rounds are canceled, orders are reassigned to other rounds
   - System prioritizes older orders for assignment
   - Admin can manually adjust assignments

3. **Round-Based Fulfillment:**
   - Volunteers view orders assigned to their rounds
   - Order processing and status updates tracked per round
   - Delivery scheduling aligned with round timing
   - Inventory management tied to round execution

### Cargo Management Flow

1. **Inventory Management:**
   - Admin can add, update, and delete cargo items
   - Support for simple quantity items and sized items (clothing)
   - Optional image upload and storage in MongoDB
   - Each item has name, description, category, and quantity tracking
   - Optional minimum quantity threshold for low stock alerts

2. **Inventory Operations:**
   - UPDATE: Change item details, quantities, availability
   - RESERVE: Temporarily reduce quantity for pending orders
   - RELEASE: Restore quantities for cancelled orders
   - COMMIT: Make quantity reduction permanent for completed orders
   - ALERT: Monitor low stock conditions based on minimum threshold

### Order-Inventory Integration

1. **Inventory Reservation System:**
   - When order is created, requested items are checked for availability
   - If available, quantities are temporarily reduced (reserved)
   - Reservation prevents overselling of inventory
   - If insufficient quantity, order creation fails with error message

2. **Status-Based Inventory Management:**
   - PENDING: Items reserved but can be released
   - PROCESSING: Items remain reserved
   - COMPLETED: Inventory deduction becomes permanent
   - CANCELLED: Reserved items returned to stock

3. **Inventory Validation and Tracking:**
   - Pre-order validation to ensure sufficient stock
   - Size-specific inventory tracking for clothing items
   - Low stock alerts when quantities fall below threshold
   - Transaction-based updates to ensure consistency

### Feedback Management

1. **Feedback Submission:**
   - Open to all users without login requirement
   - Captures name, optional phone number, and feedback content
   - Timestamp tracking for all submissions
   - No editing - submissions are immutable

2. **Admin Management:**
   - View all feedback submissions in chronological order
   - Search functionality by contributor name
   - Delete inappropriate or resolved feedback
   - No direct response mechanism in current version

### Volunteer Application Flow

1. **Application Process:**
   - User submits application with contact info and optional notes
   - System validates email uniqueness to prevent duplicate applications
   - Application stored with PENDING status and timestamp
   - Email confirmation of submission (optional feature)

2. **Admin Review:**
   - Admin views all applications grouped by status
   - Pending applications highlighted for review
   - Full application details available for evaluation
   - Approve or reject decisions with optional notes

3. **Account Creation:**
   - On approval, system automatically creates volunteer account
   - Initial password set to standard value and sent by email
   - User metadata created from application information
   - Application linked to created user account
   - Default sub-role set to REGULAR

### Access Control

1. **Authentication Requirements:**
   - Most endpoints require authentication header
   - Admin endpoints require additional admin username header
   - Some endpoints available without authentication (feedback, application submission)
   - Session-based security for sensitive operations

2. **Role-Based Access:**
   - CLIENT: Self-service operations, order creation/tracking
   - VOLUNTEER: Extended permissions for rounds and order processing
   - ADMIN: Full system access and management capabilities
   - Unauthenticated: Limited endpoints (feedback, volunteer application)

3. **Volunteer SubRole System:**
   - Regular volunteers have standard permissions
   - CLINICIAN role grants medical expertise designation
   - TEAM_LEAD role allows leadership of rounds
   - Sub-roles assigned by admin based on qualifications
   - Sub-roles affect round signup behavior and permissions

### Volunteer SubRole System

1. **SubRole Types:**
   - REGULAR: Standard volunteer (default)
   - CLINICIAN: Volunteer with medical expertise
   - TEAM_LEAD: Volunteer with leadership capabilities

2. **Role Assignment:**
   - Admin assigns sub-roles through dedicated endpoint
   - Assignment includes note about qualification basis
   - System tracks assignment date and admin who assigned role
   - Multiple role support (e.g., a volunteer can be both CLINICIAN and TEAM_LEAD)

3. **Impact on Round Participation:**
   - Each round requires exactly one TEAM_LEAD and one CLINICIAN
   - When TEAM_LEAD or CLINICIAN role volunteers sign up, they are auto-confirmed if position is available
   - REGULAR volunteers enter lottery system for remaining slots
   - Sub-role affects responsibilities during round execution

## Deployment

The application is deployed on Google Cloud run