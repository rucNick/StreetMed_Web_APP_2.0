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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Tag(name = "Authentication", description = "APIs for user authentication and profile management")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private final ObjectMapper objectMapper;
    private final SecurityManager securityManager;
    private final VolunteerSubRoleService volunteerSubRoleService;

    // Temporary token store for development - REPLACE WITH PROPER AUTH IN PRODUCTION
    // WARNING: This is NOT secure and should NEVER be used in production
    private static final Map<String, Integer> DEV_TOKEN_STORE = new ConcurrentHashMap<>();
    private static final Map<String, String> DEV_TOKEN_ROLES = new ConcurrentHashMap<>();

    // TLS configuration
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${tls.enforce.auth:false}")
    private boolean enforceHttpsForAuth;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${tls.allow.http.in.dev:false}")
    private boolean allowHttpInDev;

    // Security configuration for development
    @Value("${auth.dev.token.enabled:true}")
    private boolean devTokenEnabled;

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
        logger.info("AuthController initialized - SSL: {}, Enforce HTTPS: {}, Dev Token: {}",
                sslEnabled, enforceHttpsForAuth, devTokenEnabled);

        // Log security warning in development
        if (devTokenEnabled) {
            logger.warn("⚠️ DEV TOKEN MODE ENABLED - This is NOT secure for production!");
            logger.warn("⚠️ Using in-memory token store - tokens will be lost on restart");
        }
    }

    /**
     * Check if connection is secure (HTTPS)
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        boolean isSecure = request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Protocol"));

        logger.debug("Auth connection check - Scheme: {}, Secure: {}, Port: {}, X-Forwarded-Proto: {}",
                request.getScheme(),
                request.isSecure(),
                request.getServerPort(),
                request.getHeader("X-Forwarded-Proto"));

        return isSecure;
    }

    /**
     * Check if the connection meets security requirements
     */
    private boolean isConnectionAllowed(HttpServletRequest request, boolean isProtectedEndpoint) {
        boolean isSecure = isSecureConnection(request);

        // For protected endpoints (update operations), enforce HTTPS if configured
        if (isProtectedEndpoint && sslEnabled && enforceHttpsForAuth) {
            if (!isSecure) {
                logger.warn("Insecure auth request blocked from: {} - Endpoint: {}",
                        request.getRemoteAddr(), request.getRequestURI());
                return false;
            }
        }

        // In development, optionally allow HTTP
        if (isLocalEnvironment() && allowHttpInDev) {
            logger.debug("Local environment with HTTP allowed - accepting connection");
            return true;
        }

        // For non-protected endpoints (login/register), allow based on configuration
        if (!isProtectedEndpoint) {
            return true; // Allow login/register over HTTP in dev
        }

        // For protected endpoints, require HTTPS when SSL is enabled
        if (!isSecure && sslEnabled) {
            logger.warn("HTTP request blocked for protected endpoint when HTTPS is available");
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
     * Standard HTTPS error response
     */
    private ResponseEntity<Map<String, Object>> createHttpsRequiredResponse() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "This operation requires secure HTTPS connection");
        errorResponse.put("httpsRequired", true);
        errorResponse.put("sslEnabled", sslEnabled);
        errorResponse.put("hint", sslEnabled ? "Use HTTPS on port 8443" : "SSL is not enabled on server");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Validate authentication token (Development version)
     * WARNING: This is NOT secure - replace with JWT or proper session management in production
     */
    private boolean validateAuthToken(String authToken, Integer expectedUserId) {
        if (!devTokenEnabled) {
            // In production mode, this should validate JWT or session
            logger.error("Dev token mode disabled but no production auth implemented!");
            return false;
        }

        if (authToken == null || authToken.isEmpty()) {
            logger.warn("No auth token provided");
            return false;
        }

        Integer storedUserId = DEV_TOKEN_STORE.get(authToken);
        if (storedUserId == null) {
            logger.warn("Invalid or expired auth token");
            return false;
        }

        // If expectedUserId is provided, verify it matches
        if (expectedUserId != null && !storedUserId.equals(expectedUserId)) {
            logger.warn("Token user ID mismatch - expected: {}, actual: {}", expectedUserId, storedUserId);
            return false;
        }

        return true;
    }

    /**
     * Generate development auth token
     * WARNING: Replace with JWT in production
     */
    private String generateDevToken(Integer userId, String role) {
        String token = UUID.randomUUID().toString();
        DEV_TOKEN_STORE.put(token, userId);
        DEV_TOKEN_ROLES.put(token, role);
        logger.info("Generated dev token for user {} with role {}", userId, role);
        return token;
    }

    /**
     * Parses the request body with encryption support
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
     * Builds a ResponseEntity with encryption support
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

    @Operation(summary = "Register a new user (HTTPS recommended)")
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> register(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body,
            HttpServletRequest request) {

        // Registration is allowed over HTTP in dev, but log warning
        if (!isSecureConnection(request) && sslEnabled) {
            logger.warn("⚠️ User registration over insecure HTTP connection - should use HTTPS");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> userData = parseRequestBody(sessionId, body, "registration");

                if (userData.get("username") == null || userData.get("password") == null) {
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
                response.put("secure", isSecureConnection(request));

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

    @Operation(summary = "User login (HTTPS recommended)")
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestBody String body,
            HttpServletRequest request) {

        // Login is allowed over HTTP in dev, but log warning
        if (!isSecureConnection(request) && sslEnabled) {
            logger.warn("⚠️ User login over insecure HTTP connection - credentials may be exposed!");
        }

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

                    // Generate auth token (dev mode)
                    String authToken = null;
                    if (devTokenEnabled) {
                        authToken = generateDevToken(user.getUserId(), user.getRole());
                    }

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

                    // Include auth token if dev mode is enabled
                    if (authToken != null) {
                        response.put("authToken", authToken);
                        response.put("tokenType", "dev");
                        response.put("warning", "Dev token mode - NOT secure for production!");
                    }

                    response.put("secure", isSecureConnection(request));

                    // Enrich volunteer users with sub role details
                    if ("VOLUNTEER".equals(user.getRole())) {
                        Optional<VolunteerSubRole> subRoleOpt = volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
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

    @Operation(summary = "Update username (HTTPS required for production)")
    @PutMapping("/update/username")
    public CompletableFuture<ResponseEntity<?>> updateUsername(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        // Protected endpoint - check HTTPS requirement
        if (!isConnectionAllowed(request, true)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "username update");

                String userId = updateData.get("userId");
                String newUsername = updateData.get("newUsername");

                // Validate auth token if dev mode enabled
                if (devTokenEnabled && !validateAuthToken(authToken, Integer.parseInt(userId))) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired auth token");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
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
                response.put("secure", isSecureConnection(request));

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

    @Operation(summary = "Update phone number (HTTPS required for production)")
    @PutMapping("/update/phone")
    public CompletableFuture<ResponseEntity<?>> updatePhone(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        // Protected endpoint - check HTTPS requirement
        if (!isConnectionAllowed(request, true)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "phone update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPhone = updateData.get("newPhone");

                // Validate auth token if dev mode enabled
                if (devTokenEnabled && !validateAuthToken(authToken, Integer.parseInt(userId))) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired auth token");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
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
                response.put("secure", isSecureConnection(request));

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

    @Operation(summary = "Update password (HTTPS required)")
    @PutMapping("/update/password")
    public CompletableFuture<ResponseEntity<?>> updatePassword(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        // Password update ALWAYS requires HTTPS when SSL is enabled
        if (sslEnabled && !isSecureConnection(request)) {
            logger.error("Password update attempted over insecure connection!");
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "password update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newPassword = updateData.get("newPassword");

                // Validate auth token if dev mode enabled
                if (devTokenEnabled && !validateAuthToken(authToken, Integer.parseInt(userId))) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired auth token");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
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
                response.put("secure", isSecureConnection(request));

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

    @Operation(summary = "Update email (HTTPS required for production)")
    @PutMapping("/update/email")
    public CompletableFuture<ResponseEntity<?>> updateEmail(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody String body,
            HttpServletRequest request) {

        // Protected endpoint - check HTTPS requirement
        if (!isConnectionAllowed(request, true)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> updateData = parseRequestBody(sessionId, body, "email update");

                String userId = updateData.get("userId");
                String currentPassword = updateData.get("currentPassword");
                String newEmail = updateData.get("newEmail");

                // Validate auth token if dev mode enabled
                if (devTokenEnabled && !validateAuthToken(authToken, Integer.parseInt(userId))) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired auth token");
                    return buildResponse(sessionId, errorResponse, HttpStatus.UNAUTHORIZED);
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
                response.put("secure", isSecureConnection(request));

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

    @Operation(summary = "Logout user")
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<?>> logout(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody(required = false) Map<String, String> body) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // In dev mode, remove token from store
                if (devTokenEnabled && authToken != null) {
                    Integer userId = DEV_TOKEN_STORE.remove(authToken);
                    DEV_TOKEN_ROLES.remove(authToken);
                    if (userId != null) {
                        logger.info("User {} logged out, token invalidated", userId);
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Logged out successfully");
                response.put("authenticated", false);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error during logout: {}", e.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Logout failed");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Get authentication status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpServletRequest request) {

        Map<String, Object> status = new HashMap<>();

        if (devTokenEnabled && authToken != null) {
            Integer userId = DEV_TOKEN_STORE.get(authToken);
            String role = DEV_TOKEN_ROLES.get(authToken);

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

        status.put("secure", isSecureConnection(request));
        status.put("authMode", devTokenEnabled ? "dev-token" : "none");
        status.put("sslEnabled", sslEnabled);

        return ResponseEntity.ok(status);
    }
}