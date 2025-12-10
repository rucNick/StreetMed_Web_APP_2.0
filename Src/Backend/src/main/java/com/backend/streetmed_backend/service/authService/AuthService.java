package com.backend.streetmed_backend.service.authService;

import com.backend.streetmed_backend.dto.auth.*;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.service.volunteerService.VolunteerSubRoleService;
import com.backend.streetmed_backend.util.RequestResponseUtil;
import com.backend.streetmed_backend.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final VolunteerSubRoleService volunteerSubRoleService;
    private final TLSService tlsService;
    private final RequestResponseUtil requestResponseUtil;
    private final Executor authExecutor;

    @Autowired
    public AuthService(UserService userService,
                       VolunteerSubRoleService volunteerSubRoleService,
                       TLSService tlsService,
                       RequestResponseUtil requestResponseUtil,
                       @Qualifier("authExecutor") Executor authExecutor) {
        this.userService = userService;
        this.volunteerSubRoleService = volunteerSubRoleService;
        this.tlsService = tlsService;
        this.requestResponseUtil = requestResponseUtil;
        this.authExecutor = authExecutor;
    }

    /**
     * Register a new user
     */
    public ResponseEntity<?> register(RegisterRequest request) {
        try {
            Map<String, String> userData = request.getUserData();

            // Validate required fields
            if (isNullOrEmpty(userData.get("username")) || isNullOrEmpty(userData.get("password"))) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing required fields"));
            }

            // Build user entity
            User newUser = buildUserFromRegistration(userData);

            // Create user
            User savedUser = userService.createUser(newUser);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", savedUser.getUserId());
            response.put("secure", request.isSecure());

            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.success("User registered successfully", response));

        } catch (Exception e) {
            logger.error("Error processing registration: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.badRequest(e.getMessage()));
        }
    }

    /**
     * User login
     */
    public ResponseEntity<?> login(LoginRequest request) {
        try {
            Map<String, String> credentials = request.getCredentials();

            String usernameOrEmail = credentials.get("username");
            String password = credentials.get("password");

            // Validate credentials
            if (isNullOrEmpty(usernameOrEmail) || isNullOrEmpty(password)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing credentials"));
            }

            // Find user
            User user = findUserByUsernameOrEmail(usernameOrEmail);

            // Verify password and process login
            if (user != null && userService.verifyUserPassword(password, user.getPassword())) {
                return processSuccessfulLogin(user, request);
            } else {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Invalid credentials"));
            }

        } catch (Exception e) {
            logger.error("Error processing login: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.internalError(e.getMessage()));
        }
    }

    /**
     * Update username
     */
    public ResponseEntity<?> updateUsername(UpdateUsernameRequest request) {
        try {
            // Validate authentication
            if (!tlsService.isAuthenticated(request.getAuthToken(), null)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Invalid or expired auth token"));
            }

            Map<String, String> updateData = request.getUpdateData();
            String userId = updateData.get("userId");
            String newUsername = updateData.get("newUsername");

            // Validate input
            if (isNullOrEmpty(userId) || isNullOrEmpty(newUsername)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing required fields"));
            }

            // Check username availability
            if (userService.findByUsername(newUsername) != null) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.conflict("Username already taken"));
            }

            // Update username
            User updatedUser = userService.updateUsername(Integer.parseInt(userId), newUsername);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("username", updatedUser.getUsername());
            response.put("secure", request.isSecure());

            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.success("Username updated successfully", response));

        } catch (Exception e) {
            logger.error("Error processing username update: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.internalError(e.getMessage()));
        }
    }

    /**
     * Update phone number
     */
    public ResponseEntity<?> updatePhone(UpdatePhoneRequest request) {
        try {
            // Validate authentication
            if (!tlsService.isAuthenticated(request.getAuthToken(), null)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Invalid or expired auth token"));
            }

            Map<String, String> updateData = request.getUpdateData();
            String userId = updateData.get("userId");
            String currentPassword = updateData.get("currentPassword");
            String newPhone = updateData.get("newPhone");

            // Validate input
            if (isNullOrEmpty(userId) || isNullOrEmpty(currentPassword) || isNullOrEmpty(newPhone)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing required fields"));
            }

            // Validate user and password
            User user = validateUserAndPassword(Integer.parseInt(userId), currentPassword);
            if (user == null) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.notFound("User not found"));
            }

            // Update phone
            User updatedUser = userService.updatePhoneWithVerification(
                    Integer.parseInt(userId), currentPassword, newPhone);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("phone", updatedUser.getPhone());
            response.put("secure", request.isSecure());

            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.success("Phone number updated successfully", response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("password is incorrect")) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Current password is incorrect"));
            }
            throw e;
        } catch (Exception e) {
            logger.error("Error processing phone update: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.internalError(e.getMessage()));
        }
    }

    /**
     * Update password
     */
    public ResponseEntity<?> updatePassword(UpdatePasswordRequest request) {
        try {
            // Validate authentication
            if (!tlsService.isAuthenticated(request.getAuthToken(), null)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Invalid or expired auth token"));
            }

            Map<String, String> updateData = request.getUpdateData();
            String userId = updateData.get("userId");
            String currentPassword = updateData.get("currentPassword");
            String newPassword = updateData.get("newPassword");

            // Validate input
            if (isNullOrEmpty(userId) || isNullOrEmpty(currentPassword) || isNullOrEmpty(newPassword)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing required fields"));
            }

            // Validate user and password
            User user = validateUserAndPassword(Integer.parseInt(userId), currentPassword);
            if (user == null) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.notFound("User not found"));
            }

            // Update password
            userService.updatePasswordWithVerification(Integer.parseInt(userId), currentPassword, newPassword);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("secure", request.isSecure());

            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.success("Password updated successfully", response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("password is incorrect")) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Current password is incorrect"));
            }
            throw e;
        } catch (Exception e) {
            logger.error("Error processing password update: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.internalError(e.getMessage()));
        }
    }

    /**
     * Update email
     */
    public ResponseEntity<?> updateEmail(UpdateEmailRequest request) {
        try {
            // Validate authentication
            if (!tlsService.isAuthenticated(request.getAuthToken(), null)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Invalid or expired auth token"));
            }

            Map<String, String> updateData = request.getUpdateData();
            String userId = updateData.get("userId");
            String currentPassword = updateData.get("currentPassword");
            String newEmail = updateData.get("newEmail");

            // Validate input
            if (isNullOrEmpty(userId) || isNullOrEmpty(currentPassword) || isNullOrEmpty(newEmail)) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.badRequest("Missing required fields"));
            }

            // Validate user and password
            User user = validateUserAndPassword(Integer.parseInt(userId), currentPassword);
            if (user == null) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.notFound("User not found"));
            }

            // Check email availability
            if (userService.findByEmail(newEmail) != null) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.conflict("Email already in use"));
            }

            // Update email
            User updatedUser = userService.updateEmailWithVerification(
                    Integer.parseInt(userId), currentPassword, newEmail);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("email", updatedUser.getEmail());
            response.put("secure", request.isSecure());

            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.success("Email updated successfully", response));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("password is incorrect")) {
                return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                        ResponseUtil.unauthorized("Current password is incorrect"));
            }
            throw e;
        } catch (Exception e) {
            logger.error("Error processing email update: {}", e.getMessage(), e);
            return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                    ResponseUtil.internalError(e.getMessage()));
        }
    }

    /**
     * Logout user
     */
    public ResponseEntity<?> logout(String authToken) {
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
    }

    /**
     * Get authentication status
     */
    public ResponseEntity<Map<String, Object>> getAuthStatus(String authToken, boolean isSecure) {
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

        status.put("secure", isSecure);
        status.put("authMode", "dev-token");

        return ResponseEntity.ok(status);
    }

    // Private helper methods

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private User buildUserFromRegistration(Map<String, String> userData) {
        User newUser = new User();
        newUser.setUsername(userData.get("username"));

        String email = userData.get("email");
        if (!isNullOrEmpty(email)) {
            newUser.setEmail(email);
        }

        newUser.setPassword(userData.get("password"));

        String phone = userData.get("phone");
        if (!isNullOrEmpty(phone)) {
            newUser.setPhone(phone);
        }

        newUser.setRole("CLIENT");

        // Build metadata
        UserMetadata metadata = new UserMetadata();

        String firstName = userData.get("firstName");
        if (!isNullOrEmpty(firstName)) {
            metadata.setFirstName(firstName);
        }

        String lastName = userData.get("lastName");
        if (!isNullOrEmpty(lastName)) {
            metadata.setLastName(lastName);
        }

        newUser.setMetadata(metadata);

        return newUser;
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return userService.findByEmail(usernameOrEmail);
        } else {
            return userService.findByUsername(usernameOrEmail);
        }
    }

    private ResponseEntity<?> processSuccessfulLogin(User user, LoginRequest request) {
        // Update last login asynchronously
        CompletableFuture.runAsync(() -> userService.updateLastLogin(user.getUserId()), authExecutor);

        // Generate auth token
        String authToken = UUID.randomUUID().toString();
        tlsService.storeToken(authToken, user.getUserId(), user.getRole());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("role", user.getRole());
        response.put("username", user.getUsername());

        if (user.getEmail() != null) {
            response.put("email", user.getEmail());
        }

        // Add firstName and lastName from metadata (empty string if not set)
        UserMetadata metadata = user.getMetadata();
        if (metadata != null && metadata.getFirstName() != null && !metadata.getFirstName().isEmpty()) {
            response.put("firstName", metadata.getFirstName());
        } else {
            response.put("firstName", "");
        }
        if (metadata != null && metadata.getLastName() != null && !metadata.getLastName().isEmpty()) {
            response.put("lastName", metadata.getLastName());
        } else {
            response.put("lastName", "");
        }

        response.put("authToken", authToken);
        response.put("tokenType", "dev");
        response.put("secure", request.isSecure());

        // Add volunteer sub-role if applicable
        if ("VOLUNTEER".equals(user.getRole())) {
            Optional<VolunteerSubRole> subRoleOpt = volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
            String subRoleStr = subRoleOpt.map(vsr -> vsr.getSubRole().toString())
                    .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
            response.put("volunteerSubRole", subRoleStr);
        }

        return requestResponseUtil.buildEncryptedResponse(request.getSessionId(),
                ResponseUtil.success("Login successful", response));
    }

    private User validateUserAndPassword(Integer userId, String password) {
        User user = userService.findById(userId);

        if (user != null && !userService.verifyUserPassword(password, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        return user;
    }
}