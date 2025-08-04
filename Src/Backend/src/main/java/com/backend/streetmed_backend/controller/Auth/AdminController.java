package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.service.VolunteerSubRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Admin User Management", description = "APIs for administrators to manage users")
@RestController
@RequestMapping("/api/admin")
public class AdminController {


    private final UserService userService;
    private final Executor authExecutor;
    private final EmailService emailService;
    private final Executor readOnlyExecutor;
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
    private final SecureRandom random = new SecureRandom();
    private final VolunteerSubRoleService volunteerSubRoleService;

    @Autowired
    public AdminController(
            UserService userService,
            VolunteerSubRoleService volunteerSubRoleService,
            EmailService emailService,
            @Qualifier("authExecutor") Executor authExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.userService = userService;
        this.volunteerSubRoleService = volunteerSubRoleService;
        this.emailService = emailService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
    }


    @Operation(summary = "Update volunteer sub role (Admin only)")
    @PutMapping("/volunteer/subrole")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateVolunteerSubRole(
            @RequestBody Map<String, String> requestData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = requestData.get("adminUsername");
                String authStatus = requestData.get("authenticated");
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                Integer userId = Integer.parseInt(requestData.get("userId"));
                String subRoleStr = requestData.get("volunteerSubRole");
                String notes = requestData.get("notes");

                VolunteerSubRole.SubRoleType subRole;
                try {
                    subRole = VolunteerSubRole.SubRoleType.valueOf(subRoleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid volunteer sub role provided");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }

                VolunteerSubRole updatedSubRole = volunteerSubRoleService.assignVolunteerSubRole(userId, subRole, admin.getUserId(), notes);
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Volunteer sub role updated successfully");
                response.put("volunteerSubRole", updatedSubRole.getSubRole().toString());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

//    @Operation(summary = "Migrate all passwords to hashed format")
//    @PostMapping("/migrate-passwords")
//    public CompletableFuture<ResponseEntity<Map<String, Object>>> migratePasswords(
//            @Schema(example = "admin") @RequestHeader("Admin-Username") String adminUsername,
//            @Schema(example = "true") @RequestHeader("Authentication-Status") String authStatus) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                if (!"true".equals(authStatus)) {
//                    throw new RuntimeException("Not authenticated");
//                }
//
//                User admin = userService.findByUsername(adminUsername);
//                if (admin == null || !"ADMIN".equals(admin.getRole())) {
//                    throw new RuntimeException("Unauthorized access");
//                }
//
//                userService.migrateAllPasswordsToHashed();
//
//                Map<String, Object> response = new HashMap<>();
//                response.put("status", "success");
//                response.put("message", "All passwords have been migrated to hashed format");
//                return ResponseEntity.ok(response);
//
//            } catch (Exception e) {
//                Map<String, Object> errorResponse = new HashMap<>();
//                errorResponse.put("status", "error");
//                errorResponse.put("message", e.getMessage());
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//            }
//        }, authExecutor);
//    }

    @Operation(summary = "Get all users (Admin only)")
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllUsers(
            @Schema(example = "admin") @RequestHeader("Admin-Username") String adminUsername,
            @Schema(example = "true") @RequestHeader("Authentication-Status") String authStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    errorResponse.put("authenticated", false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    errorResponse.put("authenticated", true);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                List<User> allUsers = userService.getAllUsers();
                List<Map<String, Object>> clientUsers = new ArrayList<>();
                List<Map<String, Object>> volunteerUsers = new ArrayList<>();
                List<Map<String, Object>> adminUsers = new ArrayList<>();

                for (User user : allUsers) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getUserId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("phone", user.getPhone() != null ? user.getPhone() : "");
                    userInfo.put("role", user.getRole());

                    // If the user is a volunteer, add the volunteer sub role.
                    if ("VOLUNTEER".equals(user.getRole())) {
                        Optional<VolunteerSubRole> subRoleOpt = volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
                        String volunteerSubRoleStr = subRoleOpt
                                .map(vsr -> vsr.getSubRole().toString())
                                .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
                        userInfo.put("volunteerSubRole", volunteerSubRoleStr);
                        volunteerUsers.add(userInfo);
                    } else {
                        switch (user.getRole()) {
                            case "CLIENT" -> clientUsers.add(userInfo);
                            case "ADMIN" -> adminUsers.add(userInfo);
                            default -> {
                            }
                        }
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.put("data", Map.of(
                        "clients", clientUsers,
                        "volunteers", volunteerUsers,
                        "admins", adminUsers
                ));

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Delete user (Admin only)")
    @DeleteMapping("/user/delete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteUser(
            @Schema(example = """
                    {
                        "authenticated": "true",
                        "adminUsername": "admin",
                        "username": "usertodelete"
                    }
                    """)
            @RequestBody Map<String, String> deleteRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String isAuthenticatedStr = deleteRequest.get("authenticated");
                if (!"true".equals(isAuthenticatedStr)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    errorResponse.put("authenticated", false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                String adminUsername = deleteRequest.get("adminUsername");
                String userToDelete = deleteRequest.get("username");

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    errorResponse.put("authenticated", true);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                User userToBeDeleted = userService.findByUsername(userToDelete);
                if (userToBeDeleted == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "User not found");
                    errorResponse.put("authenticated", true);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                userService.deleteUser(userToBeDeleted.getUserId());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User deleted successfully");
                response.put("authenticated", true);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    // Update the createUser method in AdminController.java

    @Operation(summary = "Create a new user (Admin only)",
            description = "Creates a new user with CLIENT or VOLUNTEER role. Password is randomly generated.")
    @PostMapping("/user/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestBody @Schema(example = """
        {
            "adminUsername": "admin",
            "authenticated": "true",
            "username": "newuser",
            "email": "newuser@example.com",
            "phone": "412-555-0126",
            "firstName": "New",
            "lastName": "User",
            "role": "CLIENT"
        }
        """) Map<String, String> userData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = userData.get("adminUsername");
                String authStatus = userData.get("authenticated");

                // Authentication check
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Authorization check
                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                // Validate required fields
                String username = userData.get("username");
                String role = userData.get("role");

                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("Username is required");
                }

                if (role == null || role.trim().isEmpty()) {
                    throw new IllegalArgumentException("Role is required");
                }

                // Validate role - only allow CLIENT and VOLUNTEER
                if (!role.equals("CLIENT") && !role.equals("VOLUNTEER")) {
                    throw new IllegalArgumentException("Invalid role. Role must be CLIENT or VOLUNTEER");
                }

                // Email is required for VOLUNTEER roles
                String email = userData.get("email");
                if (role.equals("VOLUNTEER") && (email == null || email.trim().isEmpty())) {
                    throw new IllegalArgumentException("Email is required for VOLUNTEER role");
                }

                // Generate random password (10 characters)
                String generatedPassword = generateRandomPassword();

                // Create new user
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setPassword(generatedPassword);
                newUser.setRole(role);

                // Set email if provided
                if (email != null && !email.trim().isEmpty()) {
                    newUser.setEmail(email);
                } else {
                    // For CLIENT role, if email is not provided, use username as email
                    newUser.setEmail(username);
                }

                // Set phone if provided
                String phone = userData.get("phone");
                if (phone != null && !phone.trim().isEmpty()) {
                    newUser.setPhone(phone);
                }

                // Create and set metadata
                UserMetadata metadata = new UserMetadata();
                metadata.setFirstName(userData.get("firstName"));
                metadata.setLastName(userData.get("lastName"));
                metadata.setCreatedAt(LocalDateTime.now());
                metadata.setLastLogin(LocalDateTime.now());
                newUser.setMetadata(metadata);

                // Save user
                User savedUser = userService.createUser(newUser);

                // Send email with credentials if email is provided
                if (email != null && !email.trim().isEmpty()) {
                    emailService.sendNewUserCredentials(email, username, generatedPassword);
                }

                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User created successfully");
                response.put("userId", savedUser.getUserId());
                response.put("username", savedUser.getUsername());
                response.put("role", savedUser.getRole());
                response.put("generatedPassword", generatedPassword);

                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());

                if (e.getMessage().contains("already exists")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Update user information (Admin only)")
    @PutMapping("/user/update/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateUser(
            @PathVariable Integer userId,
            @RequestBody @Schema(example = """
            {
                "adminUsername": "admin",
                "authenticated": "true",
                "username": "updatedusername",
                "email": "updated@example.com",
                "phone": "412-555-0127",
                "role": "VOLUNTEER",
                "firstName": "Updated",
                "lastName": "Name"
            }
            """) Map<String, String> updateData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = updateData.get("adminUsername");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                User user = userService.findById(userId);
                if (user == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                // Save current username for tracking purposes
                String currentUsername = user.getUsername();
                Map<String, String> updatedFields = new HashMap<>();

                String username = updateData.get("username");
                if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
                    userService.updateUsername(userId, username);
                    updatedFields.put("username", username);
                }

                String email = updateData.get("email");
                if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
                    user.setEmail(email);
                    updatedFields.put("email", email);
                }

                String phone = updateData.get("phone");
                if (phone != null && !phone.equals(user.getPhone())) {
                    user.setPhone(phone);
                    updatedFields.put("phone", phone);
                }

                String role = updateData.get("role");
                if (role != null && !role.trim().isEmpty() && !role.equals(user.getRole())) {
                    if (!role.equals("CLIENT") && !role.equals("VOLUNTEER") && !role.equals("ADMIN")) {
                        throw new IllegalArgumentException("Invalid role. Role must be CLIENT, VOLUNTEER, or ADMIN");
                    }

                    if ((role.equals("VOLUNTEER") || role.equals("ADMIN"))
                            && (user.getEmail() == null || user.getEmail().trim().isEmpty())
                            && (email == null || email.trim().isEmpty())) {
                        throw new IllegalArgumentException("Email is required for VOLUNTEER and ADMIN roles");
                    }

                    user.setRole(role);
                    updatedFields.put("role", role);
                }

                UserMetadata metadata = user.getMetadata();
                if (metadata == null) {
                    metadata = new UserMetadata();
                    metadata.setCreatedAt(LocalDateTime.now());
                    metadata.setLastLogin(LocalDateTime.now());
                    user.setMetadata(metadata);
                }

                String firstName = updateData.get("firstName");
                if (firstName != null && !firstName.trim().isEmpty() && !firstName.equals(metadata.getFirstName())) {
                    metadata.setFirstName(firstName);
                    updatedFields.put("firstName", firstName);
                }

                String lastName = updateData.get("lastName");
                if (lastName != null && !lastName.trim().isEmpty() && !lastName.equals(metadata.getLastName())) {
                    metadata.setLastName(lastName);
                    updatedFields.put("lastName", lastName);
                }

                if (!updatedFields.isEmpty()) {
                    User updatedUser = userService.updateUser(user);

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "User updated successfully");
                    response.put("userId", updatedUser.getUserId());
                    response.put("currentUsername", currentUsername);
                    response.put("updatedFields", updatedFields);

                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "No changes were made");
                    response.put("userId", user.getUserId());
                    response.put("currentUsername", currentUsername);

                    return ResponseEntity.ok(response);
                }

            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());

                if (e.getMessage().contains("already exists")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset user password (Admin only)",
            description = "Resets a user's password to the provided new password.")
    @PutMapping("/user/reset-password/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @Parameter(description = "ID of the user whose password to reset")
            @PathVariable Integer userId,
            @RequestBody @Schema(example = """
            {
                "adminUsername": "admin",
                "authenticated": "true",
                "newPassword": "newSecurePassword123"
            }
            """) Map<String, String> resetData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = resetData.get("adminUsername");
                String authStatus = resetData.get("authenticated");
                String newPassword = resetData.get("newPassword");

                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Check admin authorization
                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                // Validate new password
                if (newPassword == null || newPassword.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "New password is required");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }

                // Get user
                User user = userService.findById(userId);
                if (user == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                // Update password with the provided one
                userService.updatePassword(userId, newPassword);

                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Password reset successfully");
                response.put("userId", user.getUserId());
                response.put("username", user.getUsername());

                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Get user details (Admin only)")
    @GetMapping("/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserDetails(
            @Parameter(description = "ID of the user to retrieve")
            @PathVariable Integer userId,
            @RequestHeader(name = "Admin-Username")
            @Parameter(description = "Username of the admin") String adminUsername,
            @RequestHeader(name = "Authentication-Status")
            @Parameter(description = "Authentication status (must be 'true')") String authStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                User user = userService.findById(userId);
                if (user == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                Map<String, Object> userDetails = new HashMap<>();
                userDetails.put("userId", user.getUserId());
                userDetails.put("username", user.getUsername());
                userDetails.put("email", user.getEmail());
                userDetails.put("phone", user.getPhone());
                userDetails.put("role", user.getRole());

                UserMetadata metadata = user.getMetadata();
                if (metadata != null) {
                    userDetails.put("firstName", metadata.getFirstName());
                    userDetails.put("lastName", metadata.getLastName());
                    userDetails.put("createdAt", metadata.getCreatedAt());
                    userDetails.put("lastLogin", metadata.getLastLogin());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", userDetails);

                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    /**
     * Generates a random password of specified length.
     */
    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARS.length());
            sb.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }
}