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
 * REST Controller for administrative operations
 * All business logic is delegated to AdminService
 * Focuses on HTTP request/response handling and security validation
 */
@Tag(name = "Admin User Management", description = "APIs for administrators to manage users (TLS/HTTPS required in production)")
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

    @Autowired
    public AdminController(AdminService adminService,
                           @Qualifier("authExecutor") Executor authExecutor,
                           @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.adminService = adminService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
        logger.info("AdminController initialized - TLS enforcement: {}", enforceHttpsForAdmin);
    }

    /**
     * Validates HTTPS requirement for admin operations
     */
    private boolean isSecureConnectionRequired(HttpServletRequest request) {
        // Skip HTTPS enforcement in development/local profiles
        if (activeProfile.contains("local") || activeProfile.contains("dev")) {
            return true;
        }

        if (!enforceHttpsForAdmin) {
            return true;
        }

        boolean isSecure = request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        if (!isSecure) {
            logger.warn("Insecure admin request blocked from: {}", request.getRemoteAddr());
        }

        return isSecure;
    }

    /**
     * Standard HTTPS error response
     */
    private ResponseEntity<Map<String, Object>> createHttpsRequiredResponse() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "Admin operations require secure HTTPS connection");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Standard error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Standard success response
     */
    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        if (data != null) {
            response.putAll(data);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update volunteer sub role (Admin only)")
    @PutMapping("/volunteer/subrole")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateVolunteerSubRole(
            @RequestBody Map<String, String> requestData,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = requestData.get("adminUsername");
                String authStatus = requestData.get("authenticated");

                if (!"true".equals(authStatus)) {
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

    @Operation(summary = "Get all users (Admin only)")
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllUsers(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
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

    @Operation(summary = "Delete user (Admin only)")
    @DeleteMapping("/user/delete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteUser(
            @RequestBody Map<String, String> deleteRequest,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

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

    @Operation(summary = "Create a new user (Admin only)")
    @PostMapping("/user/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestBody Map<String, String> userData,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = userData.get("adminUsername");
                String authStatus = userData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> createdUser = adminService.createUser(adminUsername, userData);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "User created successfully");
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

    @Operation(summary = "Update user information (Admin only)")
    @PutMapping("/user/update/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateUser(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> updateData,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = updateData.get("adminUsername");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> updateResult = adminService.updateUser(adminUsername, userId, updateData);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
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

    @Operation(summary = "Reset user password (Admin only)")
    @PutMapping("/user/reset-password/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> resetData,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = resetData.get("adminUsername");
                String authStatus = resetData.get("authenticated");
                String newPassword = resetData.get("newPassword");

                if (!"true".equals(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> resetResult = adminService.resetUserPassword(
                        adminUsername, userId, newPassword);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
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

    @Operation(summary = "Get user details (Admin only)")
    @GetMapping("/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserDetails(
            @PathVariable Integer userId,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> userDetails = adminService.getUserDetails(adminUsername, userId);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
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

    @Operation(summary = "Get user statistics (Admin only)")
    @GetMapping("/statistics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserStatistics(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (!isSecureConnectionRequired(request)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return createErrorResponse("Not authenticated", HttpStatus.UNAUTHORIZED);
                }

                Map<String, Object> statistics = adminService.getUserStatistics(adminUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
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
}