package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.security.SecurityManager;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.service.VolunteerSubRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Authentication", description = "APIs for user authentication and profile management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private final ObjectMapper objectMapper;
    private final SecurityManager securityManager;
    private final VolunteerSubRoleService volunteerSubRoleService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    public AuthController(
            UserService userService,
            VolunteerSubRoleService volunteerSubRoleService,
            @Qualifier("authExecutor") Executor authExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor,
            SecurityManager securityManager,
            ObjectMapper objectMapper) {
        this.userService = userService;
        this.volunteerSubRoleService = volunteerSubRoleService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
        this.securityManager = securityManager;
        this.objectMapper = objectMapper;
        logger.info("AuthController initialized with SecurityManager");
    }

    /**
     * Parses the request body. If a session ID is provided, it attempts to decrypt the body first.
     *
     * @param sessionId the session ID (can be null)
     * @param body the request body
     * @param logPrefix a string to identify the request type in logs
     * @return a map representing the parsed JSON data
     * @throws Exception if parsing (or decryption and then parsing) fails
     */
    private Map<String, String> parseRequestBody(String sessionId, String body, String logPrefix) throws Exception {
        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                logger.info("Received encrypted {} request for session: {}", logPrefix, sessionId);
                String decryptedBody = securityManager.decrypt(sessionId, body);
                Map<String, String> data = objectMapper.readValue(decryptedBody, Map.class);
                logger.info("Decrypted {} request", logPrefix);
                return data;
            } catch (IllegalStateException e) {
                logger.warn("Session key not found for {}: {}, trying to parse as regular JSON", logPrefix, e.getMessage());
                try {
                    return objectMapper.readValue(body, Map.class);
                } catch (Exception jsonEx) {
                    throw new RuntimeException("Failed to parse request as JSON after decryption failed for " + logPrefix, jsonEx);
                }
            }
        } else {
            return objectMapper.readValue(body, Map.class);
        }
    }

    /**
     * Builds a ResponseEntity. If a session ID and key are provided, the response body will be encrypted.
     *
     * @param sessionId the session ID (can be null)
     * @param response the response object to be serialized as JSON
     * @param status the HTTP status to be used for the response
     * @return a ResponseEntity containing either the encrypted or plain response body
     */
    private ResponseEntity<?> buildResponse(String sessionId, Object response, HttpStatus status) {
        if (sessionId != null && !sessionId.isEmpty() && securityManager.getSessionKey(sessionId) != null) {
            try {
                String encryptedResponse = securityManager.encrypt(sessionId, objectMapper.writeValueAsString(response));
                return ResponseEntity.status(status).body(encryptedResponse);
            } catch (Exception e) {
                logger.error("Error encrypting response: {}", e.getMessage());
            }
        }
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> register(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> userData = parseRequestBody(sessionId, body, "registration");

                if (userData.get("username") == null ||
                        userData.get("password") == null) {
                    throw new RuntimeException("Missing required fields");
                }

                User newUser = new User();
                newUser.setUsername(userData.get("username"));
                if (userData.containsKey("email") && userData.get("email") != null) {
                    newUser.setEmail(userData.get("email"));
                }
                newUser.setPassword(userData.get("password"));
                if (userData.containsKey("phone") && userData.get("phone") != null &&
                        !userData.get("phone").trim().isEmpty()) {
                    newUser.setPhone(userData.get("phone"));
                }
                newUser.setRole("CLIENT");

                // Create and set metadata
                UserMetadata metadata = new UserMetadata();
                if (userData.containsKey("firstName") && userData.get("firstName") != null) {
                    metadata.setFirstName(userData.get("firstName"));
                }
                if (userData.containsKey("lastName") && userData.get("lastName") != null) {
                    metadata.setLastName(userData.get("lastName"));
                }
                newUser.setMetadata(metadata);

                User savedUser = userService.createUser(newUser);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User registered successfully");
                response.put("userId", savedUser.getUserId());

                return buildResponse(sessionId, response, HttpStatus.OK);

            } catch (Exception e) {
                logger.error("Error processing registration: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.BAD_REQUEST);
            }
        }, authExecutor);
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> credentials = parseRequestBody(sessionId, body, "login");

                String usernameOrEmail = credentials.get("username");
                String password = credentials.get("password");

                if (usernameOrEmail == null || password == null) {
                    throw new RuntimeException("Missing credentials");
                }

                User user;
                if (usernameOrEmail.contains("@")) {
                    user = userService.findByEmail(usernameOrEmail);
                } else {
                    user = userService.findByUsername(usernameOrEmail);
                }

                if (user != null && userService.verifyUserPassword(password, user.getPassword())) {
                    CompletableFuture.runAsync(() -> userService.updateLastLogin(user.getUserId()), authExecutor);

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Login successful");
                    response.put("userId", user.getUserId());
                    response.put("role", user.getRole());
                    response.put("authenticated", true);
                    response.put("username", user.getUsername());
                    if (user.getEmail() != null) {
                        response.put("email", user.getEmail());
                    }

                    // Enrich volunteer users with sub role details
                    if ("VOLUNTEER".equals(user.getRole())) {
                        Optional<VolunteerSubRole> subRoleOpt = volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
                        // Default to REGULAR if no sub role is assigned.
                        String subRoleStr = subRoleOpt.map(vsr -> vsr.getSubRole().toString())
                                .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
                        response.put("volunteerSubRole", subRoleStr);
                    }

                    return buildResponse(sessionId, response, HttpStatus.OK);
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid credentials");
                    errorResponse.put("authenticated", false);
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                logger.error("Error processing login: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, readOnlyExecutor);
    }


    @Operation(summary = "Update username")
    @PutMapping("/update/username")
    public CompletableFuture<ResponseEntity<?>> updateUsername(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "username update");

                String userId = updateData.get("userId");
                String newUsername = updateData.get("newUsername");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                if (userId == null || newUsername == null) {
                    throw new RuntimeException("Missing required fields");
                }

                if (userService.findByUsername(newUsername) != null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Username already taken");
                    return buildResponse(sessionId, errorResponse, HttpStatus.CONFLICT);
                }

                User updatedUser = userService.updateUsername(Integer.parseInt(userId), newUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Username updated successfully");
                response.put("username", updatedUser.getUsername());

                return buildResponse(sessionId, response, HttpStatus.OK);

            } catch (Exception e) {
                logger.error("Error processing username update: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Update phone number")
    @PutMapping("/update/phone")
    public CompletableFuture<ResponseEntity<?>> updatePhone(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "phone update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPhone = updateData.get("newPhone");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                if (userId == null || currentPassword == null || newPhone == null) {
                    throw new RuntimeException("Missing required fields");
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Current password is incorrect");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
                }

                User updatedUser = userService.updatePhoneWithVerification(
                        Integer.parseInt(userId),
                        currentPassword,
                        newPhone
                );

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Phone number updated successfully");
                response.put("phone", updatedUser.getPhone());

                return buildResponse(sessionId, response, HttpStatus.OK);

            } catch (Exception e) {
                logger.error("Error processing phone update: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Update password")
    @PutMapping("/update/password")
    public CompletableFuture<ResponseEntity<?>> updatePassword(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "password update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPassword = updateData.get("newPassword");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                if (userId == null || currentPassword == null || newPassword == null) {
                    throw new RuntimeException("Missing required fields");
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Current password is incorrect");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
                }

                userService.updatePasswordWithVerification(Integer.parseInt(userId), currentPassword, newPassword);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Password updated successfully");

                return buildResponse(sessionId, response, HttpStatus.OK);

            } catch (Exception e) {
                logger.error("Error processing password update: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Update email")
    @PutMapping("/update/email")
    public CompletableFuture<ResponseEntity<?>> updateEmail(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "email update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newEmail = updateData.get("newEmail");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                if (userId == null || currentPassword == null || newEmail == null) {
                    throw new RuntimeException("Missing required fields");
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    throw new RuntimeException("User not found");
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Current password is incorrect");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
                }

                if (userService.findByEmail(newEmail) != null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Email already in use");
                    return buildResponse(sessionId, errorResponse, HttpStatus.CONFLICT);
                }

                User updatedUser = userService.updateEmailWithVerification(Integer.parseInt(userId), currentPassword, newEmail);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Email updated successfully");
                response.put("email", updatedUser.getEmail());

                return buildResponse(sessionId, response, HttpStatus.OK);

            } catch (Exception e) {
                logger.error("Error processing email update: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return buildResponse(sessionId, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }
}
