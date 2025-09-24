package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.AdminService;
import com.backend.streetmed_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Admin User Management", description = "APIs for administrators to manage users")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private final TLSService tlsService;

    @Autowired
    public AdminController(AdminService adminService,
                           @Qualifier("authExecutor") Executor authExecutor,
                           @Qualifier("readOnlyExecutor") Executor readOnlyExecutor,
                           TLSService tlsService) {
        this.adminService = adminService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
        this.tlsService = tlsService;
    }

    @Operation(summary = "Update volunteer sub role")
    @PutMapping("/volunteer/subrole")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateVolunteerSubRole(
            @RequestBody Map<String, String> requestData,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = requestData.get("adminUsername");
                String authStatus = requestData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Integer userId = Integer.parseInt(requestData.get("userId"));
                String subRoleStr = requestData.get("volunteerSubRole");
                String notes = requestData.get("notes");

                VolunteerSubRole updatedSubRole = adminService.updateVolunteerSubRole(
                        adminUsername, userId, subRoleStr, notes);

                Map<String, Object> data = new HashMap<>();
                data.put("volunteerSubRole", updatedSubRole.getSubRole().toString());

                return ResponseUtil.success("Volunteer sub role updated successfully", data);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (IllegalArgumentException e) {
                return ResponseUtil.badRequest(e.getMessage());
            } catch (Exception e) {
                logger.error("Error updating volunteer sub-role: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllUsers(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, List<Map<String, Object>>> groupedUsers =
                        adminService.getAllUsersGroupedByRole(adminUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("data", groupedUsers);
                response.put("secure", tlsService.isSecureConnection(request));

                return ResponseUtil.successData(response);

            } catch (SecurityException e) {
                return ResponseUtil.error(e.getMessage(), HttpStatus.FORBIDDEN, true);
            } catch (Exception e) {
                logger.error("Error retrieving users: {}", e.getMessage());
                return ResponseUtil.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, true);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/user/delete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteUser(
            @RequestBody Map<String, String> deleteRequest,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String isAuthenticatedStr = deleteRequest.get("authenticated");
                if (!"true".equals(isAuthenticatedStr)) {
                    return ResponseUtil.unauthorized();
                }

                String adminUsername = deleteRequest.get("adminUsername");
                String userToDelete = deleteRequest.get("username");

                adminService.deleteUser(adminUsername, userToDelete);

                return ResponseUtil.success("User deleted successfully");

            } catch (SecurityException e) {
                return ResponseUtil.error(e.getMessage(), HttpStatus.FORBIDDEN, true);
            } catch (IllegalArgumentException e) {
                return ResponseUtil.error(e.getMessage(), HttpStatus.NOT_FOUND, true);
            } catch (Exception e) {
                logger.error("Error deleting user: {}", e.getMessage());
                return ResponseUtil.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, true);
            }
        }, authExecutor);
    }

    @Operation(summary = "Create a new user")
    @PostMapping("/user/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestBody Map<String, String> userData,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = userData.get("adminUsername");
                String authStatus = userData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, Object> createdUser = adminService.createUser(adminUsername, userData);

                return ResponseUtil.success("User created successfully", createdUser);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("already exists") ?
                        ResponseUtil.conflict(e.getMessage()) :
                        ResponseUtil.badRequest(e.getMessage());
            } catch (Exception e) {
                logger.error("Error creating user: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Update user information")
    @PutMapping("/user/update/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateUser(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> updateData,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = updateData.get("adminUsername");
                String authStatus = updateData.get("authenticated");

                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, Object> updateResult = adminService.updateUser(adminUsername, userId, updateData);

                return ResponseUtil.successData(updateResult);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("not found")) {
                    return ResponseUtil.notFound(e.getMessage());
                } else if (e.getMessage().contains("already exists")) {
                    return ResponseUtil.conflict(e.getMessage());
                } else {
                    return ResponseUtil.badRequest(e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error updating user: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset user password")
    @PutMapping("/user/reset-password/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> resetData,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = resetData.get("adminUsername");
                String authStatus = resetData.get("authenticated");
                String newPassword = resetData.get("newPassword");

                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, Object> resetResult = adminService.resetUserPassword(
                        adminUsername, userId, newPassword);

                return ResponseUtil.successData(resetResult);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("not found") ?
                        ResponseUtil.notFound(e.getMessage()) :
                        ResponseUtil.badRequest(e.getMessage());
            } catch (Exception e) {
                logger.error("Error resetting password: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Get user details")
    @GetMapping("/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserDetails(
            @PathVariable Integer userId,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, Object> userDetails = adminService.getUserDetails(adminUsername, userId);

                Map<String, Object> response = new HashMap<>();
                response.put("data", userDetails);

                return ResponseUtil.successData(response);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (IllegalArgumentException e) {
                return ResponseUtil.notFound(e.getMessage());
            } catch (Exception e) {
                logger.error("Error retrieving user details: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get user statistics")
    @GetMapping("/statistics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserStatistics(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, true)) {
            return CompletableFuture.completedFuture(ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    return ResponseUtil.unauthorized();
                }

                Map<String, Object> statistics = adminService.getUserStatistics(adminUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("data", statistics);

                return ResponseUtil.successData(response);

            } catch (SecurityException e) {
                return ResponseUtil.forbidden(e.getMessage());
            } catch (Exception e) {
                logger.error("Error retrieving statistics: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, readOnlyExecutor);
    }

    /**
     * DEV ONLY - Creates a default admin user without authentication
     * Username: admin, Password: admin
     * WARNING: Remove or disable this endpoint in production!
     */
    @Operation(summary = "Create default admin user (DEV ONLY - NO AUTH REQUIRED)")
    @PostMapping("/dev/quick-admin")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createQuickAdminUser(
            HttpServletRequest request) {

        logger.warn("DEV ENDPOINT: Creating admin user without authentication!");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare admin user data with minimal required fields
                Map<String, String> adminData = new HashMap<>();
                adminData.put("username", "admin");
                adminData.put("password", "admin");
                adminData.put("email", "");  //
                adminData.put("firstName", "");  // blank
                adminData.put("lastName", "");  // blank
                adminData.put("role", "ADMIN");
                adminData.put("phone", "");  // blank

                // Bypass authentication by using a system identifier
                // The service will need to handle this special case
                Map<String, Object> createdUser = adminService.createUser("SYSTEM", adminData);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Admin user created successfully (DEV MODE)");
                response.put("username", "admin");
                response.put("password", "admin");
                response.put("warning", "This is a DEV endpoint - DO NOT use in production!");

                logger.info("Dev admin user created: username=admin");
                return ResponseUtil.success("Admin user created (DEV)", response);

            } catch (Exception e) {
                logger.error("Failed to create dev admin user: {}", e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                    return ResponseUtil.conflict("Admin user already exists");
                }

                return ResponseUtil.internalError("Failed to create admin user: " + e.getMessage());
            }
        }, authExecutor);
    }
}