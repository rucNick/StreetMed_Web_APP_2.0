package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.repository.User.UserRepository;
import com.backend.streetmed_backend.security.PasswordHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to initialize a default admin account on application startup if none exists.
 * This ensures there's always at least one admin account to manage the system.
 */
@Component
@Order(1) // Ensures this runs early in the startup sequence
public class AdminInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializationService.class);
    private static final String DEFAULT_ADMIN_USERNAME = "Admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "test@test.streetmed.com";

    private final UserRepository userRepository;
    private final PasswordHash passwordHash;

    @Autowired
    public AdminInitializationService(UserRepository userRepository, PasswordHash passwordHash) {
        this.userRepository = userRepository;
        this.passwordHash = passwordHash;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // Check if any admin exists in the database
            boolean adminExists = userRepository.existsByRole("ADMIN");

            if (!adminExists) {
                logger.info("No admin account found in database. Creating default admin account...");
                createDefaultAdmin();
            } else {
                logger.info("Admin account(s) found in database. Skipping default admin creation.");
            }
        } catch (Exception e) {
            logger.error("Error during admin initialization: {}", e.getMessage(), e);
            // Don't throw exception to allow application to continue starting
        }
    }

    /**
     * Creates the default admin account with predefined credentials
     */
    private void createDefaultAdmin() {
        try {
            // Check if username already exists (extra safety check)
            if (userRepository.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
                logger.warn("Username '{}' already exists but is not an admin. Skipping creation.", DEFAULT_ADMIN_USERNAME);
                return;
            }

            // Create new admin user
            User adminUser = new User();
            adminUser.setUsername(DEFAULT_ADMIN_USERNAME);
            adminUser.setPassword(passwordHash.hashPassword(DEFAULT_ADMIN_PASSWORD));
            adminUser.setEmail(DEFAULT_ADMIN_EMAIL);
            adminUser.setRole("ADMIN");
            adminUser.setPhone(""); // Optional: Set a default phone number if needed

            // Save user first to get the generated ID
            User savedAdmin = userRepository.save(adminUser);

            // Create and set metadata
            UserMetadata metadata = new UserMetadata();
            metadata.setFirstName("System");
            metadata.setLastName("Administrator");
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setLastLogin(LocalDateTime.now());
            metadata.setUser(savedAdmin);

            savedAdmin.setMetadata(metadata);

            // Save with metadata
            userRepository.save(savedAdmin);

            logger.info("========================================");
            logger.info("Default admin account created successfully!");
            logger.info("Username: {}", DEFAULT_ADMIN_USERNAME);
            logger.info("Password: {}", DEFAULT_ADMIN_PASSWORD);
            logger.info("*** IMPORTANT: Please change the default password immediately ***");
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Failed to create default admin account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create default admin account", e);
        }
    }
}