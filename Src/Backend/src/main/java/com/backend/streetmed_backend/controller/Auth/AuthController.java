package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.service.volunteerService.VolunteerSubRoleService;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Authentication", description = "APIs for user authentication and profile management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private final VolunteerSubRoleService volunteerSubRoleService;
    private final TLSService tlsService;
    private final RequestResponseUtil requestResponseUtil;

    @Autowired
    public AuthController(
            UserService userService,
            VolunteerSubRoleService volunteerSubRoleService,
            @Qualifier("authExecutor") Executor authExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor,
            TLSService tlsService,
            RequestResponseUtil requestResponseUtil) {
        this.userService = userService;
        this.volunteerSubRoleService = volunteerSubRoleService;
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
            try {
                Map<String, String> userData = requestResponseUtil.parseRequestBody(sessionId, body, "registration");

                if (userData.get("username") == null || userData.get("password") == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing required fields"));
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
                response.put("userId", savedUser.getUserId());
                response.put("secure", tlsService.isSecureConnection(request));

                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("User registered successfully", response));

            } catch (Exception e) {
                logger.error("Error processing registration: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.badRequest(e.getMessage()));
            }
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
            try {
                Map<String, String> credentials = requestResponseUtil.parseRequestBody(sessionId, body, "login");

                String usernameOrEmail = credentials.get("username");
                String password = credentials.get("password");

                if (usernameOrEmail == null || password == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing credentials"));
                }

                User user;
                if (usernameOrEmail.contains("@")) {
                    user = userService.findByEmail(usernameOrEmail);
                } else {
                    user = userService.findByUsername(usernameOrEmail);
                }

                if (user != null && userService.verifyUserPassword(password, user.getPassword())) {
                    CompletableFuture.runAsync(() -> userService.updateLastLogin(user.getUserId()), authExecutor);

                    String authToken = UUID.randomUUID().toString();
                    tlsService.storeToken(authToken, user.getUserId(), user.getRole());

                    Map<String, Object> response = new HashMap<>();
                    response.put("userId", user.getUserId());
                    response.put("role", user.getRole());
                    response.put("username", user.getUsername());
                    if (user.getEmail() != null) {
                        response.put("email", user.getEmail());
                    }
                    response.put("authToken", authToken);
                    response.put("tokenType", "dev");
                    response.put("secure", tlsService.isSecureConnection(request));

                    if ("VOLUNTEER".equals(user.getRole())) {
                        Optional<VolunteerSubRole> subRoleOpt = volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
                        String subRoleStr = subRoleOpt.map(vsr -> vsr.getSubRole().toString())
                                .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
                        response.put("volunteerSubRole", subRoleStr);
                    }

                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.success("Login successful", response));
                } else {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Invalid credentials"));
                }
            } catch (Exception e) {
                logger.error("Error processing login: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError(e.getMessage()));
            }
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
            try {
                Map<String, String> updateData = requestResponseUtil.parseRequestBody(sessionId, body, "username update");

                if (!tlsService.isAuthenticated(authToken, null)) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Invalid or expired auth token"));
                }

                String userId = updateData.get("userId");
                String newUsername = updateData.get("newUsername");

                if (userId == null || newUsername == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing required fields"));
                }

                if (userService.findByUsername(newUsername) != null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.conflict("Username already taken"));
                }

                User updatedUser = userService.updateUsername(Integer.parseInt(userId), newUsername);

                Map<String, Object> response = new HashMap<>();
                response.put("username", updatedUser.getUsername());
                response.put("secure", tlsService.isSecureConnection(request));

                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("Username updated successfully", response));

            } catch (Exception e) {
                logger.error("Error processing username update: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError(e.getMessage()));
            }
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
            try {
                Map<String, String> updateData = requestResponseUtil.parseRequestBody(sessionId, body, "phone update");

                if (!tlsService.isAuthenticated(authToken, null)) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Invalid or expired auth token"));
                }

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPhone = updateData.get("newPhone");

                if (userId == null || currentPassword == null || newPhone == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing required fields"));
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.notFound("User not found"));
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Current password is incorrect"));
                }

                User updatedUser = userService.updatePhoneWithVerification(
                        Integer.parseInt(userId), currentPassword, newPhone);

                Map<String, Object> response = new HashMap<>();
                response.put("phone", updatedUser.getPhone());
                response.put("secure", tlsService.isSecureConnection(request));

                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("Phone number updated successfully", response));

            } catch (Exception e) {
                logger.error("Error processing phone update: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError(e.getMessage()));
            }
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
            try {
                Map<String, String> updateData = requestResponseUtil.parseRequestBody(sessionId, body, "password update");

                if (!tlsService.isAuthenticated(authToken, null)) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Invalid or expired auth token"));
                }

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPassword = updateData.get("newPassword");

                if (userId == null || currentPassword == null || newPassword == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing required fields"));
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.notFound("User not found"));
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Current password is incorrect"));
                }

                userService.updatePasswordWithVerification(Integer.parseInt(userId), currentPassword, newPassword);

                Map<String, Object> response = new HashMap<>();
                response.put("secure", tlsService.isSecureConnection(request));

                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("Password updated successfully", response));

            } catch (Exception e) {
                logger.error("Error processing password update: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError(e.getMessage()));
            }
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
            try {
                Map<String, String> updateData = requestResponseUtil.parseRequestBody(sessionId, body, "email update");

                if (!tlsService.isAuthenticated(authToken, null)) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Invalid or expired auth token"));
                }

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newEmail = updateData.get("newEmail");

                if (userId == null || currentPassword == null || newEmail == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.badRequest("Missing required fields"));
                }

                User user = userService.findById(Integer.parseInt(userId));
                if (user == null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.notFound("User not found"));
                }

                if (!userService.verifyUserPassword(currentPassword, user.getPassword())) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.unauthorized("Current password is incorrect"));
                }

                if (userService.findByEmail(newEmail) != null) {
                    return requestResponseUtil.buildEncryptedResponse(sessionId,
                            ResponseUtil.conflict("Email already in use"));
                }

                User updatedUser = userService.updateEmailWithVerification(
                        Integer.parseInt(userId), currentPassword, newEmail);

                Map<String, Object> response = new HashMap<>();
                response.put("email", updatedUser.getEmail());
                response.put("secure", tlsService.isSecureConnection(request));

                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.success("Email updated successfully", response));

            } catch (Exception e) {
                logger.error("Error processing email update: {}", e.getMessage(), e);
                return requestResponseUtil.buildEncryptedResponse(sessionId,
                        ResponseUtil.internalError(e.getMessage()));
            }
        }, authExecutor);
    }

    @Operation(summary = "Logout user")
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<?>> logout(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody(required = false) Map<String, String> body) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken != null) {
                    tlsService.removeToken(authToken);
                    logger.info("Token invalidated for logout");
                }
                return ResponseUtil.success("Logged out successfully");

            } catch (Exception e) {
                logger.error("Error during logout: {}", e.getMessage());
                return ResponseUtil.internalError("Logout failed");
            }
        }, authExecutor);
    }

    @Operation(summary = "Get authentication status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpServletRequest request) {

        Map<String, Object> status = new HashMap<>();

        if (authToken != null) {
            Integer userId = tlsService.getUserIdFromToken(authToken);
            String role = tlsService.getRoleFromToken(authToken);

            if (userId != null) {
                status.put("authenticated", true);
                status.put("userId", userId);
                status.put("role", role);
                status.put("tokenValid", true);
            } else {
                status.put("authenticated", false);
                status.put("tokenValid", false);
                status.put("message", "Invalid or expired token");
            }
        } else {
            status.put("authenticated", false);
            status.put("message", "No authentication token provided");
        }

        status.put("secure", tlsService.isSecureConnection(request));
        status.put("authMode", "dev-token");

        return ResponseEntity.ok(status);
    }
}