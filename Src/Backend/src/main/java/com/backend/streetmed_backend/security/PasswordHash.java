package com.backend.streetmed_backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHash {
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordHash() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public String hashPassword(String plainTextPassword) {
        return passwordEncoder.encode(plainTextPassword);
    }

    public boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        return passwordEncoder.matches(plainTextPassword, hashedPassword);
    }
}