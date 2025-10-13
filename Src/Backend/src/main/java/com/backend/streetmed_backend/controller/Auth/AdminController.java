package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.dto.admin.*;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.adminService.AdminService;
import com.backend.streetmed_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody UpdateVolunteerSubRoleRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() ->
                adminService.updateVolunteerSubRole(request), authExecutor);
    }

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllUsers(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            GetAllUsersRequest request = new GetAllUsersRequest(adminUsername, authStatus);
            return adminService.getAllUsersGroupedByRole(request,
                    tlsService.isSecureConnection(httpRequest));
        }, readOnlyExecutor);
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/user/delete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteUser(
            @RequestBody DeleteUserRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() ->
                adminService.deleteUser(request), authExecutor);
    }

    @Operation(summary = "Create a new user")
    @PostMapping("/user/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createUser(
            @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() ->
                adminService.createUser(request), authExecutor);
    }

    @Operation(summary = "Update user information")
    @PutMapping("/user/update/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateUser(
            @PathVariable Integer userId,
            @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        request.setUserId(userId);
        return CompletableFuture.supplyAsync(() ->
                adminService.updateUser(request), authExecutor);
    }

    @Operation(summary = "Reset user password")
    @PutMapping("/user/reset-password/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @PathVariable Integer userId,
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        request.setUserId(userId);
        return CompletableFuture.supplyAsync(() ->
                adminService.resetUserPassword(request), authExecutor);
    }

    @Operation(summary = "Get user details")
    @GetMapping("/user/{userId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserDetails(
            @PathVariable Integer userId,
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            GetUserDetailsRequest request = new GetUserDetailsRequest(adminUsername, authStatus, userId);
            return adminService.getUserDetails(request);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get user statistics")
    @GetMapping("/statistics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getUserStatistics(
            @RequestHeader("Admin-Username") String adminUsername,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            GetStatisticsRequest request = new GetStatisticsRequest(adminUsername, authStatus);
            return adminService.getUserStatistics(request);
        }, readOnlyExecutor);
    }
}