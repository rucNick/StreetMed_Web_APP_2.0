package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Admin Controller with proper TLS/HTTPS enforcement
 * All sensitive operations require HTTPS connection in production
 */
@Tag(name = "Admin User Management", description = "APIs for administrators to manage users (HTTPS/TLS required)")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;

    // TLS configuration
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${tls.enforce.admin:true}")
    private boolean enforceHttpsForAdmin;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${tls.allow.http.in.dev:false}")
    private boolean allowHttpInDev;

    @Autowired
    public AdminController(AdminService adminService,
                           @Qualifier("authExecutor") Executor authExecutor,
                           @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.adminService = adminService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
        logger.info("AdminController initialized - SSL: {}, Enforce HTTPS: {}, Profile: {}",
                sslEnabled, enforceHttpsForAdmin, activeProfile);
    }

    /**
     * Validates HTTPS requirement for admin operations
     * Fixed to properly check for secure connections
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        // Check multiple indicators of HTTPS connection
        boolean isSecure = request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Protocol"));

        // Log detailed connection info for debugging
        logger.debug("Connection check - Scheme: {}, Secure: {}, Port: {}, X-Forwarded-Proto: {}",
                request.getScheme(),
                request.isSecure(),
                request.getServerPort(),
                request.getHeader("X-Forwarded-Proto"));

        return isSecure;
    }

    /**
     * Check if the connection meets security requirements
     */
    private boolean isConnectionAllowed(HttpServletRequest request) {
        boolean isSecure = isSecureConnection(request);

        // If SSL is enabled and HTTPS enforcement is on, require HTTPS
        if (sslEnabled && enforceHttpsForAdmin) {
            if (!isSecure) {
                logger.warn("Insecure admin request blocked from: {} - Scheme: {}, Port: {}",
                        request.getRemoteAddr(),
                        request.getScheme(),
                        request.getServerPort());
                return false;
            }
        }

        // In development, optionally allow HTTP (controlled by config)
        if (isLocalEnvironment() && allowHttpInDev) {
            logger.debug("Local environment with HTTP allowed - accepting connection");
            return true;
        }

        // For production or when HTTP is not explicitly allowed, require HTTPS
        if (!isSecure && sslEnabled) {
            logger.warn("HTTP request blocked when HTTPS is available");
            return false;
        }

        return true;
    }

    /**
     * Check if running in local environment
     */
    private boolean isLocalEnvironment() {
        return activeProfile.contains("local") ||
                activeProfile.contains("dev") ||
                activeProfile.contains("default");
    }

    /**
     * Validate admin authentication
     */
    private boolean validateAdminAuth(String authStatus) {
        return "true".equals(authStatus);
    }

    /**
     * Standard HTTPS error response
     */
    private ResponseEntity<Map<String, Object>> createHttpsRequiredResponse() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "Admin operations require secure HTTPS connection");
        errorResponse.put("httpsRequired", true);
        errorResponse.put("sslEnabled", sslEnabled);
        errorResponse.put("hint", sslEnabled ? "Use HTTPS on port 8443" : "SSL is not enabled on server");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Standard error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("authenticated", false);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Standard success response
     */
    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("authenticated", true);
        if (data != null) {
            response.putAll(data);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update volunteer sub role (Admin only - HTTPS required)")
    @PutMapping("/volunteer/subrole")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateVolunteerSubRole(
            @RequestBody Map<String, String> requestData,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = requestData.get("adminUsername");
                String authStatus = requestData.get("authenticated");

                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Integer userId = Integer.parseInt(requestData.get("userId"));
                String subRoleStr = requestData.get("volunteerSubRole");
                String notes = requestData.get("notes");

                VolunteerSubRole updatedSubRole = adminService.updateVolunteerSubRole(
                        adminUsername, userId, subRoleStr, notes);

                Map<String, Object> data = new HashMap<>();
                data.put("volunteerSubRole", updatedSubRole.getSubRole().toString());

                return createSuccessResponse("Volunteer sub role updated successfully", data);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                logger.error("Error updating volunteer sub-role: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Get all users (Admin only - HTTPS required)")
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllUsers(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!validateAdminAuth(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    errorResponse.put("authenticated", false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                Map<String, List<Map<String, Object>>> groupedUsers =
                        adminService.getAllUsersGroupedByRole(adminUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.put("data", groupedUsers);
                response.put("secure", isSecureConnection(request));  // Add security indicator

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } catch (Exception e) {
                logger.error("Error retrieving users: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Delete user (Admin only - HTTPS required)")
    @DeleteMapping("/user/delete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteUser(
            @RequestBody Map<String, String> deleteRequest,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String isAuthenticatedStr = deleteRequest.get("authenticated");
                if (!validateAdminAuth(isAuthenticatedStr)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    errorResponse.put("authenticated", false);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                String adminUsername = deleteRequest.get("adminUsername");
                String userToDelete = deleteRequest.get("username");

                adminService.deleteUser(adminUsername, userToDelete);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User deleted successfully");
                response.put("authenticated", true);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } catch (Exception e) {
                logger.error("Error deleting user: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("authenticated", true);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Create a new user (Admin only - HTTPS required)")
    @PostMapping("/user/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestBody Map<String, String> userData,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = userData.get("adminUsername");
                String authStatus = userData.get("authenticated");

                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> createdUser = adminService.createUser(adminUsername, userData);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User created successfully");
                response.put("authenticated", true);
                response.putAll(createdUser);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(e.getMessage(),
                        e.getMessage().contains("already exists") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                logger.error("Error creating user: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Update user information (Admin only - HTTPS required)")
    @PutMapping("/user/update/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateUser(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> updateData,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = updateData.get("adminUsername");
                String authStatus = updateData.get("authenticated");

                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> updateResult = adminService.updateUser(adminUsername, userId, updateData);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.putAll(updateResult);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (IllegalArgumentException e) {
                HttpStatus status = e.getMessage().contains("not found") ? HttpStatus.NOT_FOUND :
                        e.getMessage().contains("already exists") ? HttpStatus.CONFLICT :
                                HttpStatus.BAD_REQUEST;
                return createErrorResponse(e.getMessage(), status);
            } catch (Exception e) {
                logger.error("Error updating user: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset user password (Admin only - HTTPS required)")
    @PutMapping("/user/reset-password/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> resetData,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = resetData.get("adminUsername");
                String authStatus = resetData.get("authenticated");
                String newPassword = resetData.get("newPassword");

                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> resetResult = adminService.resetUserPassword(
                        adminUsername, userId, newPassword);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.putAll(resetResult);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (IllegalArgumentException e) {
                HttpStatus status = e.getMessage().contains("not found") ?
                        HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
                return createErrorResponse(e.getMessage(), status);
            } catch (Exception e) {
                logger.error("Error resetting password: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, authExecutor);
    }

    @Operation(summary = "Get user details (Admin only - HTTPS required)")
    @GetMapping("/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserDetails(
            @PathVariable Integer userId,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> userDetails = adminService.getUserDetails(adminUsername, userId);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.put("data", userDetails);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                logger.error("Error retrieving user details: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get user statistics (Admin only - HTTPS required)")
    @GetMapping("/statistics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserStatistics(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isConnectionAllowed(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!validateAdminAuth(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> statistics = adminService.getUserStatistics(adminUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("authenticated", true);
                response.put("data", statistics);

                return ResponseEntity.ok(response);

            } catch (SecurityException e) {
                return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (Exception e) {
                logger.error("Error retrieving statistics: {}", e.getMessage());
                return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get TLS/Security status for this controller")
    @GetMapping("/security-status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        status.put("sslEnabled", sslEnabled);
        status.put("enforceHttpsForAdmin", enforceHttpsForAdmin);
        status.put("currentConnectionSecure", isSecureConnection(request));
        status.put("connectionAllowed", isConnectionAllowed(request));
        status.put("requestScheme", request.getScheme());
        status.put("requestSecure", request.isSecure());
        status.put("xForwardedProto", request.getHeader("X-Forwarded-Proto"));
        status.put("serverPort", request.getServerPort());
        status.put("environment", activeProfile);
        status.put("allowHttpInDev", allowHttpInDev);
        status.put("tlsRequired", sslEnabled && enforceHttpsForAdmin);

        logger.info("Security status check - Secure: {}, Allowed: {}, Scheme: {}, Port: {}",
                isSecureConnection(request), isConnectionAllowed(request),
                request.getScheme(), request.getServerPort());

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Hash a plain password (Dev utility - HTTPS required)")
    @PostMapping("/utility/hash-password")
    public ResponseEntity<Map<String, Object>> hashPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Only allow in local/dev environment
        if (!isLocalEnvironment()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "This endpoint is only available in development environment");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Still check for HTTPS if configured
        if (!isConnectionAllowed(httpRequest)) {
            return createHttpsRequiredResponse();
        }

        String plainPassword = request.get("password");
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Password is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            // Use BCryptPasswordEncoder to hash the password
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
            String hashedPassword = encoder.encode(plainPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("plainPassword", plainPassword);
            response.put("hashedPassword", hashedPassword);
            response.put("algorithm", "BCrypt");
            response.put("strength", 12);
            response.put("note", "Use this hash in your database for the password field");

            logger.info("Password hashed successfully for development use");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error hashing password: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to hash password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Verify a password against a hash (Dev utility)")
    @PostMapping("/utility/verify-password")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Only allow in local/dev environment
        if (!isLocalEnvironment()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "This endpoint is only available in development environment");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        String plainPassword = request.get("password");
        String hashedPassword = request.get("hash");

        if (plainPassword == null || hashedPassword == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Both password and hash are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            boolean matches = encoder.matches(plainPassword, hashedPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("matches", matches);
            response.put("message", matches ? "Password matches the hash" : "Password does NOT match the hash");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error verifying password: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to verify password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}