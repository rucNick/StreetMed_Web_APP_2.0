package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for administrative operations
 * Handles all business logic for admin functionality
 */
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
     * Validates if a user has admin privileges
     *
     * @param username The username to check
     * @return true if user is an admin, false otherwise
     */
    public boolean validateAdminAccess(String username) {
        if (username == null) {
            logger.warn("Admin validation attempted with null username");
            return true;
        }

        User admin = userService.findByUsername(username);
        boolean isAdmin = admin != null && "ADMIN".equals(admin.getRole());

        if (!isAdmin) {
            logger.warn("Unauthorized admin access attempt by user: {}", username);
        }

        return !isAdmin;
    }

    /**
     * Updates volunteer sub-role
     *
     * @param adminUsername Admin performing the operation
     * @param userId User ID to update
     * @param subRoleStr Sub-role string value
     * @param notes Optional notes
     * @return Updated VolunteerSubRole
     */
    public VolunteerSubRole updateVolunteerSubRole(String adminUsername, Integer userId,
                                                   String subRoleStr, String notes) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        User admin = userService.findByUsername(adminUsername);

        VolunteerSubRole.SubRoleType subRole;
        try {
            subRole = VolunteerSubRole.SubRoleType.valueOf(subRoleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid volunteer sub role provided: " + subRoleStr);
        }

        VolunteerSubRole updatedSubRole = volunteerSubRoleService.assignVolunteerSubRole(
                userId, subRole, admin.getUserId(), notes);

        logger.info("Volunteer sub-role updated for user {} to {} by admin {}",
                userId, subRole, adminUsername);

        return updatedSubRole;
    }

    /**
     * Retrieves all users grouped by role
     *
     * @param adminUsername Admin performing the operation
     * @return Map of users grouped by role
     */
    public Map<String, List<Map<String, Object>>> getAllUsersGroupedByRole(String adminUsername) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        List<User> allUsers = userService.getAllUsers();

        List<Map<String, Object>> clientUsers = new ArrayList<>();
        List<Map<String, Object>> volunteerUsers = new ArrayList<>();
        List<Map<String, Object>> adminUsers = new ArrayList<>();

        for (User user : allUsers) {
            Map<String, Object> userInfo = createUserInfoMap(user);

            switch (user.getRole()) {
                case "CLIENT" -> clientUsers.add(userInfo);
                case "VOLUNTEER" -> {
                    // Add volunteer sub-role information
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

        logger.info("Retrieved all users for admin {}", adminUsername);

        Map<String, List<Map<String, Object>>> groupedUsers = new HashMap<>();
        groupedUsers.put("clients", clientUsers);
        groupedUsers.put("volunteers", volunteerUsers);
        groupedUsers.put("admins", adminUsers);

        return groupedUsers;
    }

    /**
     * Deletes a user
     *
     * @param adminUsername Admin performing the operation
     * @param usernameToDelete Username of user to delete
     */
    public void deleteUser(String adminUsername, String usernameToDelete) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        User userToDelete = userService.findByUsername(usernameToDelete);
        if (userToDelete == null) {
            throw new IllegalArgumentException("User not found: " + usernameToDelete);
        }

        // Prevent admin from deleting themselves
        if (adminUsername.equals(usernameToDelete)) {
            throw new IllegalArgumentException("Cannot delete your own admin account");
        }

        userService.deleteUser(userToDelete.getUserId());
        logger.info("User {} deleted by admin {}", usernameToDelete, adminUsername);
    }

    /**
     * Creates a new user with generated password
     *
     * @param adminUsername Admin performing the operation
     * @param userData User data map
     * @return Created user information including generated password
     */
    public Map<String, Object> createUser(String adminUsername, Map<String, String> userData) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        // Validate required fields
        String username = userData.get("username");
        String role = userData.get("role");

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required");
        }

        if (!role.equals("CLIENT") && !role.equals("VOLUNTEER")) {
            throw new IllegalArgumentException("Invalid role. Role must be CLIENT or VOLUNTEER");
        }

        String email = userData.get("email");
        if (role.equals("VOLUNTEER") && (email == null || email.trim().isEmpty())) {
            throw new IllegalArgumentException("Email is required for VOLUNTEER role");
        }

        // Check if username already exists
        if (userService.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Generate password
        String generatedPassword = generateRandomPassword();

        // Create new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(generatedPassword);
        newUser.setRole(role);

        if (email != null && !email.trim().isEmpty()) {
            newUser.setEmail(email);
        } else {
            newUser.setEmail(username); // Default email to username for CLIENT role
        }

        String phone = userData.get("phone");
        if (phone != null && !phone.trim().isEmpty()) {
            newUser.setPhone(phone);
        }

        // Create and set metadata
        UserMetadata metadata = new UserMetadata();
        metadata.setFirstName(userData.get("firstName"));
        metadata.setLastName(userData.get("lastName"));
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setLastLogin(LocalDateTime.now());
        newUser.setMetadata(metadata);

        User savedUser = userService.createUser(newUser);

        // Send email with credentials if email is provided
        if (email != null && !email.trim().isEmpty()) {
            try {
                emailService.sendNewUserCredentials(email, username, generatedPassword);
            } catch (Exception e) {
                logger.error("Failed to send credentials email to {}: {}", email, e.getMessage());
            }
        }

        logger.info("New user {} created by admin {}", username, adminUsername);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("userId", savedUser.getUserId());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        response.put("generatedPassword", generatedPassword);
        response.put("email", savedUser.getEmail());

        return response;
    }

    /**
     * Updates user information
     *
     * @param adminUsername Admin performing the operation
     * @param userId User ID to update
     * @param updateData Update data map
     * @return Map containing update results
     */
    public Map<String, Object> updateUser(String adminUsername, Integer userId,
                                          Map<String, String> updateData) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        String currentUsername = user.getUsername();
        Map<String, String> updatedFields = new HashMap<>();

        // Update username if provided
        String newUsername = updateData.get("username");
        if (newUsername != null && !newUsername.trim().isEmpty() &&
                !newUsername.equals(user.getUsername())) {

            // Check if new username is already taken
            if (userService.findByUsername(newUsername) != null) {
                throw new IllegalArgumentException("Username already exists: " + newUsername);
            }

            userService.updateUsername(userId, newUsername);
            updatedFields.put("username", newUsername);
        }

        // Update email
        String email = updateData.get("email");
        if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
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
        if (role != null && !role.trim().isEmpty() && !role.equals(user.getRole())) {
            validateRoleUpdate(role, user, email);
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

        response.put("currentUsername", currentUsername);
        return response;
    }

    /**
     * Resets a user's password
     *
     * @param adminUsername Admin performing the operation
     * @param userId User ID whose password to reset
     * @param newPassword New password
     * @return Map containing reset results
     */
    public Map<String, Object> resetUserPassword(String adminUsername, Integer userId,
                                                 String newPassword) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        userService.updatePassword(userId, newPassword);
        logger.info("Password reset for user {} by admin {}", user.getUsername(), adminUsername);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());
        response.put("message", "Password reset successfully");

        return response;
    }

    /**
     * Gets detailed information for a specific user
     *
     * @param adminUsername Admin performing the operation
     * @param userId User ID to retrieve
     * @return User details map
     */
    public Map<String, Object> getUserDetails(String adminUsername, Integer userId) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        Map<String, Object> userDetails = createDetailedUserInfoMap(user);

        logger.info("User details retrieved for {} by admin {}", user.getUsername(), adminUsername);

        return userDetails;
    }

    /**
     * Generates a random password
     *
     * @return Generated password string
     */
    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARS.length());
            sb.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * Creates a basic user info map
     */
    private Map<String, Object> createUserInfoMap(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userInfo.put("role", user.getRole());
        return userInfo;
    }

    /**
     * Creates a detailed user info map including metadata
     */
    private Map<String, Object> createDetailedUserInfoMap(User user) {
        Map<String, Object> userDetails = createUserInfoMap(user);

        UserMetadata metadata = user.getMetadata();
        if (metadata != null) {
            userDetails.put("firstName", metadata.getFirstName());
            userDetails.put("lastName", metadata.getLastName());
            userDetails.put("createdAt", metadata.getCreatedAt());
            userDetails.put("lastLogin", metadata.getLastLogin());
        }

        // Add volunteer sub-role if applicable
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

    /**
     * Validates role update
     */
    private void validateRoleUpdate(String newRole, User user, String email) {
        if (!newRole.equals("CLIENT") && !newRole.equals("VOLUNTEER") && !newRole.equals("ADMIN")) {
            throw new IllegalArgumentException("Invalid role. Role must be CLIENT, VOLUNTEER, or ADMIN");
        }

        if ((newRole.equals("VOLUNTEER") || newRole.equals("ADMIN")) &&
                (user.getEmail() == null || user.getEmail().trim().isEmpty()) &&
                (email == null || email.trim().isEmpty())) {
            throw new IllegalArgumentException("Email is required for VOLUNTEER and ADMIN roles");
        }
    }

    /**
     * Updates user metadata
     */
    private void updateUserMetadata(User user, Map<String, String> updateData,
                                    Map<String, String> updatedFields) {
        UserMetadata metadata = user.getMetadata();
        if (metadata == null) {
            metadata = new UserMetadata();
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setLastLogin(LocalDateTime.now());
            user.setMetadata(metadata);
        }

        String firstName = updateData.get("firstName");
        if (firstName != null && !firstName.trim().isEmpty() &&
                !firstName.equals(metadata.getFirstName())) {
            metadata.setFirstName(firstName);
            updatedFields.put("firstName", firstName);
        }

        String lastName = updateData.get("lastName");
        if (lastName != null && !lastName.trim().isEmpty() &&
                !lastName.equals(metadata.getLastName())) {
            metadata.setLastName(lastName);
            updatedFields.put("lastName", lastName);
        }
    }

    /**
     * Gets statistics about users in the system
     *
     * @param adminUsername Admin performing the operation
     * @return Map containing user statistics
     */
    public Map<String, Object> getUserStatistics(String adminUsername) {
        if (validateAdminAccess(adminUsername)) {
            throw new SecurityException("Unauthorized access");
        }

        List<User> allUsers = userService.getAllUsers();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("clientCount", allUsers.stream().filter(u -> "CLIENT".equals(u.getRole())).count());
        stats.put("volunteerCount", allUsers.stream().filter(u -> "VOLUNTEER".equals(u.getRole())).count());
        stats.put("adminCount", allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count());

        logger.info("User statistics retrieved by admin {}", adminUsername);

        return stats;
    }
}