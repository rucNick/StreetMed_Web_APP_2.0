package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.dto.auth.*;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.authService.AuthService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.util.RequestResponseUtil;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Authentication", description = "APIs for user authentication and profile management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserService userService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private final TLSService tlsService;
    private final RequestResponseUtil requestResponseUtil;

    @Autowired
    public AuthController(
            AuthService authService,
            UserService userService,
            @Qualifier("authExecutor") Executor authExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor,
            TLSService tlsService,
            RequestResponseUtil requestResponseUtil) {
        this.authService = authService;
        this.userService = userService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
        this.tlsService = tlsService;
        this.requestResponseUtil = requestResponseUtil;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> register(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body,
            HttpServletRequest request) {

        if (!tlsService.isSecureConnection(request)) {
            logger.warn("⚠️ User registration over insecure HTTP connection");
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> userData;
            try {
                userData = requestResponseUtil.parseRequestBody(sessionId, body, "registration");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            RegisterRequest registerRequest = new RegisterRequest(userData, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.register(registerRequest);
        }, authExecutor);
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body,
            HttpServletRequest request) {

        if (!tlsService.isSecureConnection(request)) {
            logger.warn("⚠️ User login over insecure HTTP connection");
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> credentials;
            try {
                credentials = requestResponseUtil.parseRequestBody(sessionId, body, "login");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LoginRequest loginRequest = new LoginRequest(credentials, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.login(loginRequest);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Update username")
    @PutMapping("/update/username")
    public CompletableFuture<ResponseEntity<?>> updateUsername(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, false)) {
            return CompletableFuture.completedFuture(
                    requestResponseUtil.buildEncryptedResponse(sessionId, ResponseUtil.httpsRequired()));
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> updateData;
            try {
                updateData = requestResponseUtil.parseRequestBody(sessionId, body, "username update");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            UpdateUsernameRequest updateRequest = new UpdateUsernameRequest(updateData, authToken, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.updateUsername(updateRequest);
        }, authExecutor);
    }

    @Operation(summary = "Update phone number")
    @PutMapping("/update/phone")
    public CompletableFuture<ResponseEntity<?>> updatePhone(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, false)) {
            return CompletableFuture.completedFuture(
                    requestResponseUtil.buildEncryptedResponse(sessionId, ResponseUtil.httpsRequired()));
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> updateData;
            try {
                updateData = requestResponseUtil.parseRequestBody(sessionId, body, "phone update");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            UpdatePhoneRequest updateRequest = new UpdatePhoneRequest(updateData, authToken, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.updatePhone(updateRequest);
        }, authExecutor);
    }

    @Operation(summary = "Update password")
    @PutMapping("/update/password")
    public CompletableFuture<ResponseEntity<?>> updatePassword(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        if (!tlsService.isSecureConnection(request)) {
            logger.error("Password update attempted over insecure connection!");
            return CompletableFuture.completedFuture(
                    requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.httpsRequired("Password updates require HTTPS")));
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> updateData = null;
            try {
                updateData = requestResponseUtil.parseRequestBody(sessionId, body, "password update");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            UpdatePasswordRequest updateRequest = new UpdatePasswordRequest(updateData, authToken, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.updatePassword(updateRequest);
        }, authExecutor);
    }

    @Operation(summary = "Update email")
    @PutMapping("/update/email")
    public CompletableFuture<ResponseEntity<?>> updateEmail(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        if (tlsService.isHttpsRequired(request, false)) {
            return CompletableFuture.completedFuture(
                    requestResponseUtil.buildEncryptedResponse(sessionId, ResponseUtil.httpsRequired()));
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> updateData;
            try {
                updateData = requestResponseUtil.parseRequestBody(sessionId, body, "email update");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            UpdateEmailRequest updateRequest = new UpdateEmailRequest(updateData, authToken, sessionId,
                    tlsService.isSecureConnection(request));
            return authService.updateEmail(updateRequest);
        }, authExecutor);
    }

    @Operation(summary = "Update user name (first and last)")
    @PutMapping("/update/name")
    public CompletableFuture<ResponseEntity<?>> updateName(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> updateData;
            try {
                updateData = requestResponseUtil.parseRequestBody(sessionId, body, "name update");
            } catch (Exception e) {
                logger.error("Failed to parse name update request", e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.badRequest("Invalid request format"));
            }

            try {
                String userIdStr = updateData.get("userId");
                String firstName = updateData.get("firstName");
                String lastName = updateData.get("lastName");

                if (userIdStr == null || userIdStr.trim().isEmpty()) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("User ID is required"));
                }

                Integer userId = Integer.parseInt(userIdStr);

                User user = userService.findById(userId);
                if (user == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.notFound("User not found"));
                }

                UserMetadata metadata = user.getMetadata();
                if (metadata == null) {
                    metadata = new UserMetadata();
                    metadata.setUser(user);
                    user.setMetadata(metadata);
                }

                if (firstName != null && !firstName.trim().isEmpty()) {
                    metadata.setFirstName(firstName.trim());
                }
                if (lastName != null && !lastName.trim().isEmpty()) {
                    metadata.setLastName(lastName.trim());
                }

                userService.updateUser(user);

                Map<String, Object> response = new HashMap<>();
                response.put("firstName", metadata.getFirstName());
                response.put("lastName", metadata.getLastName());
                response.put("secure", tlsService.isSecureConnection(request));

                logger.info("Name updated for user: {}", userId);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("Name updated successfully", response));

            } catch (NumberFormatException e) {
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.badRequest("Invalid user ID format"));
            } catch (Exception e) {
                logger.error("Error updating name", e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError("Failed to update name: " + e.getMessage()));
            }
        }, authExecutor);
    }

    @Operation(summary = "Logout user")
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<?>> logout(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody(required = false) Map<String, String> body) {

        return CompletableFuture.supplyAsync(() ->
                authService.logout(authToken), authExecutor);
    }

    @Operation(summary = "Get authentication status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpServletRequest request) {

        return authService.getAuthStatus(authToken, tlsService.isSecureConnection(request));
    }
}