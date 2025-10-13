package com.backend.streetmed_backend.service.adminService;

import com.backend.streetmed_backend.dto.admin.*;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.service.volunteerService.VolunteerSubRoleService;
import com.backend.streetmed_backend.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
    private final SecureRandom random = new SecureRandom();

    private final UserService userService;
    private final VolunteerSubRoleService volunteerSubRoleService;
    private final EmailService emailService;

    @Autowired
    public AdminService(UserService userService,
                        VolunteerSubRoleService volunteerSubRoleService,
                        EmailService emailService) {
        this.userService = userService;
        this.volunteerSubRoleService = volunteerSubRoleService;
        this.emailService = emailService;
    }

    /**
     * Updates volunteer sub-role with full request validation and response building
     */
    public ResponseEntity<Map<String, Object>> updateVolunteerSubRole(UpdateVolunteerSubRoleRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthenticated())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Parse and validate sub-role
            VolunteerSubRole.SubRoleType subRole;
            try {
                subRole = VolunteerSubRole.SubRoleType.valueOf(request.getVolunteerSubRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseUtil.badRequest("Invalid volunteer sub role provided: " + request.getVolunteerSubRole());
            }

            // Perform the update
            User admin = userService.findByUsername(request.getAdminUsername());
            VolunteerSubRole updatedSubRole = volunteerSubRoleService.assignVolunteerSubRole(
                    request.getUserId(), subRole, admin.getUserId(), request.getNotes());

            logger.info("Volunteer sub-role updated for user {} to {} by admin {}",
                    request.getUserId(), subRole, request.getAdminUsername());

            // Build response
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
    }

    /**
     * Gets all users grouped by role with authentication and response handling
     */
    public ResponseEntity<Map<String, Object>> getAllUsersGroupedByRole(GetAllUsersRequest request, boolean isSecure) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthStatus())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Get users grouped by role
            Map<String, List<Map<String, Object>>> groupedUsers = fetchUsersGroupedByRole();

            logger.info("Retrieved all users for admin {}", request.getAdminUsername());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("data", groupedUsers);
            response.put("secure", isSecure);

            return ResponseUtil.successData(response);

        } catch (SecurityException e) {
            return ResponseUtil.error(e.getMessage(), HttpStatus.FORBIDDEN, true);
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage());
            return ResponseUtil.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, true);
        }
    }

    /**
     * Deletes a user with full validation and response handling
     */
    public ResponseEntity<Map<String, Object>> deleteUser(DeleteUserRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthenticated())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Validate user exists
            User userToDelete = userService.findByUsername(request.getUsername());
            if (userToDelete == null) {
                return ResponseUtil.error("User not found: " + request.getUsername(), HttpStatus.NOT_FOUND, true);
            }

            // Prevent self-deletion
            if (request.getAdminUsername().equals(request.getUsername())) {
                return ResponseUtil.error("Cannot delete your own admin account", HttpStatus.FORBIDDEN, true);
            }

            // Delete user
            userService.deleteUser(userToDelete.getUserId());
            logger.info("User {} deleted by admin {}", request.getUsername(), request.getAdminUsername());

            return ResponseUtil.success("User deleted successfully");

        } catch (SecurityException e) {
            return ResponseUtil.error(e.getMessage(), HttpStatus.FORBIDDEN, true);
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return ResponseUtil.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, true);
        }
    }

    /**
     * Creates a new user with full validation and response handling
     */
    public ResponseEntity<Map<String, Object>> createUser(CreateUserRequest request) {
        try {
            if (!isAuthenticated(request.getAuthenticated())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access (special handling for SYSTEM user)
            if (!"SYSTEM".equals(request.getAdminUsername())) {
                validateAdminAccess(request.getAdminUsername());
            }

            Map<String, String> userData = request.getUserData();
            String username = userData.get("username");
            String role = userData.get("role");
            String email = userData.get("email");

            // Validation
            if (isNullOrEmpty(username)) {
                return ResponseUtil.badRequest("Username is required");
            }

            if (isNullOrEmpty(role)) {
                return ResponseUtil.badRequest("Role is required");
            }

            if (!isValidRole(role)) {
                return ResponseUtil.badRequest("Invalid role. Role must be CLIENT, VOLUNTEER, or ADMIN");
            }

            if ("VOLUNTEER".equals(role) && isNullOrEmpty(email)) {
                return ResponseUtil.badRequest("Email is required for VOLUNTEER role");
            }

            // Check username uniqueness
            if (userService.findByUsername(username) != null) {
                return ResponseUtil.conflict("Username already exists: " + username);
            }
            // Create user
            Map<String, Object> createdUser = performUserCreation(userData, request.getAdminUsername());

            return ResponseUtil.success("User created successfully", createdUser);

        } catch (SecurityException e) {
            return ResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    /**
     * Updates user information with full validation and response handling
     */
    public ResponseEntity<Map<String, Object>> updateUser(UpdateUserRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthenticated())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Find user
            User user = userService.findById(request.getUserId());
            if (user == null) {
                return ResponseUtil.notFound("User not found with ID: " + request.getUserId());
            }

            // Perform update
            Map<String, Object> updateResult = performUserUpdate(user, request.getUpdateData(),
                    request.getAdminUsername());

            return ResponseUtil.successData(updateResult);

        } catch (SecurityException e) {
            return ResponseUtil.forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseUtil.conflict(e.getMessage());
            }
            return ResponseUtil.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    /**
     * Resets user password with full validation and response handling
     */
    public ResponseEntity<Map<String, Object>> resetUserPassword(ResetPasswordRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthenticated())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Validate new password
            if (isNullOrEmpty(request.getNewPassword())) {
                return ResponseUtil.badRequest("New password is required");
            }

            // Find user
            User user = userService.findById(request.getUserId());
            if (user == null) {
                return ResponseUtil.notFound("User not found with ID: " + request.getUserId());
            }

            // Reset password
            userService.updatePassword(request.getUserId(), request.getNewPassword());
            logger.info("Password reset for user {} by admin {}", user.getUsername(), request.getAdminUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("message", "Password reset successfully");

            return ResponseUtil.successData(response);

        } catch (SecurityException e) {
            return ResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    /**
     * Gets user details with authentication and response handling
     */
    public ResponseEntity<Map<String, Object>> getUserDetails(GetUserDetailsRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthStatus())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Get user details
            User user = userService.findById(request.getUserId());
            if (user == null) {
                return ResponseUtil.notFound("User not found with ID: " + request.getUserId());
            }

            Map<String, Object> userDetails = createDetailedUserInfoMap(user);
            logger.info("User details retrieved for {} by admin {}", user.getUsername(), request.getAdminUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("data", userDetails);

            return ResponseUtil.successData(response);

        } catch (SecurityException e) {
            return ResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving user details: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    /**
     * Gets user statistics with authentication and response handling
     */
    public ResponseEntity<Map<String, Object>> getUserStatistics(GetStatisticsRequest request) {
        try {
            // Validate authentication
            if (isAuthenticated(request.getAuthStatus())) {
                return ResponseUtil.unauthorized();
            }

            // Validate admin access
            validateAdminAccess(request.getAdminUsername());

            // Get statistics
            Map<String, Object> stats = calculateUserStatistics();
            logger.info("User statistics retrieved by admin {}", request.getAdminUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("data", stats);

            return ResponseUtil.successData(response);

        } catch (SecurityException e) {
            return ResponseUtil.forbidden(e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving statistics: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    // Private helper methods

    private boolean isAuthenticated(String authStatus) {
        return !"true".equals(authStatus);
    }

    private void validateAdminAccess(String username) throws SecurityException {
        if (username == null) {
            throw new SecurityException("Admin username is required");
        }

        User admin = userService.findByUsername(username);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            logger.warn("Unauthorized admin access attempt by user: {}", username);
            throw new SecurityException("Unauthorized access");
        }
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isValidRole(String role) {
        return !"CLIENT".equals(role) && !"VOLUNTEER".equals(role) && !"ADMIN".equals(role);
    }

    private Map<String, List<Map<String, Object>>> fetchUsersGroupedByRole() {
        List<User> allUsers = userService.getAllUsers();

        List<Map<String, Object>> clientUsers = new ArrayList<>();
        List<Map<String, Object>> volunteerUsers = new ArrayList<>();
        List<Map<String, Object>> adminUsers = new ArrayList<>();

        for (User user : allUsers) {
            Map<String, Object> userInfo = createUserInfoMap(user);

            switch (user.getRole()) {
                case "CLIENT" -> clientUsers.add(userInfo);
                case "VOLUNTEER" -> {
                    Optional<VolunteerSubRole> subRoleOpt =
                            volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
                    String volunteerSubRoleStr = subRoleOpt
                            .map(vsr -> vsr.getSubRole().toString())
                            .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
                    userInfo.put("volunteerSubRole", volunteerSubRoleStr);
                    volunteerUsers.add(userInfo);
                }
                case "ADMIN" -> adminUsers.add(userInfo);
                default -> logger.warn("Unknown role encountered: {}", user.getRole());
            }
        }

        Map<String, List<Map<String, Object>>> groupedUsers = new HashMap<>();
        groupedUsers.put("clients", clientUsers);
        groupedUsers.put("volunteers", volunteerUsers);
        groupedUsers.put("admins", adminUsers);

        return groupedUsers;
    }

    private Map<String, Object> performUserCreation(Map<String, String> userData, String adminUsername) {
        // Get or generate password
        String rawPassword = userData.get("password");
        if (isNullOrEmpty(rawPassword)) {
            rawPassword = generateRandomPassword();
        }

        // Store the generated/provided password for email
        String passwordForEmail = rawPassword;

        // Build user entity
        User newUser = new User();
        newUser.setUsername(userData.get("username"));
        newUser.setPassword(rawPassword);
        newUser.setRole(userData.get("role"));

        // Set email - use username as fallback for CLIENT role
        String email = userData.get("email");
        if (!isNullOrEmpty(email)) {
            newUser.setEmail(email);
        } else if ("CLIENT".equals(userData.get("role"))) {
            newUser.setEmail(userData.get("username"));
        }

        // Set phone if provided
        String phone = userData.get("phone");
        if (!isNullOrEmpty(phone)) {
            newUser.setPhone(phone);
        }

        // Create metadata
        UserMetadata metadata = new UserMetadata();
        String firstName = userData.get("firstName");
        if (!isNullOrEmpty(firstName)) {
            metadata.setFirstName(firstName);
        }

        String lastName = userData.get("lastName");
        if (!isNullOrEmpty(lastName)) {
            metadata.setLastName(lastName);
        }

        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setLastLogin(LocalDateTime.now());
        newUser.setMetadata(metadata);

        // Save user (UserService will handle password hashing)
        User savedUser = userService.createUser(newUser);

        // Send email notification if email exists
        if (!isNullOrEmpty(savedUser.getEmail()) && !savedUser.getEmail().equals(savedUser.getUsername())) {
            try {
                emailService.sendNewUserCredentials(
                        savedUser.getEmail(),
                        savedUser.getUsername(),
                        passwordForEmail
                );
            } catch (Exception e) {
                logger.error("Failed to send credentials email to {}: {}",
                        savedUser.getEmail(), e.getMessage());
            }
        }

        logger.info("New user {} created by admin {}", savedUser.getUsername(), adminUsername);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", savedUser.getUserId());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        response.put("generatedPassword", passwordForEmail);
        if (!isNullOrEmpty(savedUser.getEmail())) {
            response.put("email", savedUser.getEmail());
        }

        return response;
    }

    private Map<String, Object> performUserUpdate(User user, Map<String, String> updateData, String adminUsername) {
        String currentUsername = user.getUsername();
        Map<String, String> updatedFields = new HashMap<>();

        // Update username
        String newUsername = updateData.get("username");
        if (!isNullOrEmpty(newUsername) && !newUsername.equals(user.getUsername())) {
            if (userService.findByUsername(newUsername) != null) {
                throw new IllegalArgumentException("Username already exists: " + newUsername);
            }
            userService.updateUsername(user.getUserId(), newUsername);
            updatedFields.put("username", newUsername);
        }

        // Update email
        String email = updateData.get("email");
        if (!isNullOrEmpty(email) && !email.equals(user.getEmail())) {
            user.setEmail(email);
            updatedFields.put("email", email);
        }

        // Update phone
        String phone = updateData.get("phone");
        if (phone != null && !phone.equals(user.getPhone())) {
            user.setPhone(phone);
            updatedFields.put("phone", phone);
        }

        // Update role
        String role = updateData.get("role");
        if (!isNullOrEmpty(role) && !role.equals(user.getRole())) {
            if (isValidRole(role)) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
            user.setRole(role);
            updatedFields.put("role", role);
        }

        // Update metadata
        updateUserMetadata(user, updateData, updatedFields);

        Map<String, Object> response = new HashMap<>();
        if (!updatedFields.isEmpty()) {
            User updatedUser = userService.updateUser(user);
            logger.info("User {} updated by admin {}. Fields updated: {}",
                    currentUsername, adminUsername, updatedFields.keySet());

            response.put("message", "User updated successfully");
            response.put("userId", updatedUser.getUserId());
            response.put("updatedFields", updatedFields);
        } else {
            response.put("message", "No changes were made");
            response.put("userId", user.getUserId());
        }

        return response;
    }

    private void updateUserMetadata(User user, Map<String, String> updateData, Map<String, String> updatedFields) {
        UserMetadata metadata = user.getMetadata();
        if (metadata == null) {
            metadata = new UserMetadata();
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setLastLogin(LocalDateTime.now());
            user.setMetadata(metadata);
        }

        String firstName = updateData.get("firstName");
        if (!isNullOrEmpty(firstName) && !firstName.equals(metadata.getFirstName())) {
            metadata.setFirstName(firstName);
            updatedFields.put("firstName", firstName);
        }

        String lastName = updateData.get("lastName");
        if (!isNullOrEmpty(lastName) && !lastName.equals(metadata.getLastName())) {
            metadata.setLastName(lastName);
            updatedFields.put("lastName", lastName);
        }
    }

    private Map<String, Object> calculateUserStatistics() {
        List<User> allUsers = userService.getAllUsers();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("clientCount", allUsers.stream().filter(u -> "CLIENT".equals(u.getRole())).count());
        stats.put("volunteerCount", allUsers.stream().filter(u -> "VOLUNTEER".equals(u.getRole())).count());
        stats.put("adminCount", allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count());

        return stats;
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARS.length());
            sb.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }

    private Map<String, Object> createUserInfoMap(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userInfo.put("role", user.getRole());
        return userInfo;
    }

    private Map<String, Object> createDetailedUserInfoMap(User user) {
        Map<String, Object> userDetails = createUserInfoMap(user);

        UserMetadata metadata = user.getMetadata();
        if (metadata != null) {
            userDetails.put("firstName", metadata.getFirstName());
            userDetails.put("lastName", metadata.getLastName());
            userDetails.put("createdAt", metadata.getCreatedAt());
            userDetails.put("lastLogin", metadata.getLastLogin());
        }

        if ("VOLUNTEER".equals(user.getRole())) {
            Optional<VolunteerSubRole> subRoleOpt =
                    volunteerSubRoleService.getVolunteerSubRole(user.getUserId());
            String volunteerSubRoleStr = subRoleOpt
                    .map(vsr -> vsr.getSubRole().toString())
                    .orElse(VolunteerSubRole.SubRoleType.REGULAR.toString());
            userDetails.put("volunteerSubRole", volunteerSubRoleStr);
        }

        return userDetails;
    }
}