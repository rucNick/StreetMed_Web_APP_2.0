package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.repository.User.UserRepository;
import com.backend.streetmed_backend.security.PasswordHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordHash passwordHash;

    @Autowired
    public UserService(UserRepository userRepository, PasswordHash passwordHash) {
        this.userRepository = userRepository;
        this.passwordHash = passwordHash;
    }

    @Transactional
    public User createUser(User user) {
        validateNewUser(user);
        // Create new user with hashed password
        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordHash.hashPassword(user.getPassword()));
        newUser.setPhone(user.getPhone());
        newUser.setRole(user.getRole());

        // First save the user to generate the userId
        User savedUser = userRepository.save(newUser);

        // Now create metadata and associate it with the saved user
        UserMetadata metadata = new UserMetadata();
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setLastLogin(LocalDateTime.now());

        // Add this part to handle firstName and lastName from the request
        if (user.getMetadata() != null) {
            metadata.setFirstName(user.getMetadata().getFirstName());
            metadata.setLastName(user.getMetadata().getLastName());
        }

        metadata.setUser(savedUser);
        savedUser.setMetadata(metadata);
        // Save everything again
        return userRepository.saveAndFlush(savedUser);
    }

    public boolean verifyUserPassword(String plainTextPassword, String hashedPassword) {
        return passwordHash.verifyPassword(plainTextPassword, hashedPassword);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User findById(Integer userId) {
        return userRepository.findById(userId)
                .orElse(null);
    }

    @Transactional
    public void updateLastLogin(Integer userId) {
        userRepository.findById(userId).ifPresent(user -> {
            UserMetadata metadata = user.getMetadata();
            if (metadata != null) {
                metadata.setLastLogin(LocalDateTime.now());
            }
        });
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    private void validateNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(User user) {
        if (!userRepository.existsById(user.getUserId())) {
            throw new RuntimeException("User not found");
        }
        return userRepository.save(user);
    }

    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public void migrateAllPasswordsToHashed() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            String hashedPassword = passwordHash.hashPassword(user.getPassword());
            user.setPassword(hashedPassword);
            userRepository.save(user);
        }
    }

    // New methods for profile management

    @Transactional
    public User updateUsername(Integer userId, String newUsername) {
        // Check if new username is already taken
        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Username already exists");
        }

        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        user.setUsername(newUsername);
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(Integer userId, String newPassword) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Hash and set new password
        user.setPassword(passwordHash.hashPassword(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User updatePasswordWithVerification(Integer userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Verify current password
        if (!verifyUserPassword(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Hash and set new password
        user.setPassword(passwordHash.hashPassword(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User updateEmail(Integer userId, String newEmail) {
        // Check if new email is already taken
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already exists");
        }

        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    @Transactional
    public User updatePhoneWithVerification(Integer userId, String currentPassword, String newPhone) {
        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Verify current password for security
        if (!verifyUserPassword(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPhone(newPhone);
        return userRepository.save(user);
    }

    @Transactional
    public User updateEmailWithVerification(Integer userId, String currentPassword, String newEmail) {
        // Check if new email is already taken
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already exists");
        }

        User user = findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Verify current password
        if (!verifyUserPassword(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setEmail(newEmail);
        return userRepository.save(user);
    }
}