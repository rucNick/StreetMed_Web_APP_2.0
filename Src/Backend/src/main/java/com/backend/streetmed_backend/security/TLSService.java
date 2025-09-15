package com.backend.streetmed_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized authentication service to handle token validation and TLS checks
 * Eliminates redundancy across controllers
 */
@Service
public class TLSService {

    private static final Logger logger = LoggerFactory.getLogger(TLSService.class);

    // Shared token stores - in production, use Redis or database
    private static final Map<String, Integer> TOKEN_USER_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> TOKEN_ROLE_MAP = new ConcurrentHashMap<>();

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${tls.enforce.admin:true}")
    private boolean enforceHttpsForAdmin;

    @Value("${tls.allow.http.in.dev:false}")
    private boolean allowHttpInDev;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${auth.dev.token.enabled:true}")
    private boolean devTokenEnabled;

    /**
     * Store authentication token
     */
    public void storeToken(String token, Integer userId, String role) {
        TOKEN_USER_MAP.put(token, userId);
        TOKEN_ROLE_MAP.put(token, role);
        logger.debug("Token stored for user {} with role {}", userId, role);
    }

    /**
     * Remove authentication token
     */
    public void removeToken(String token) {
        Integer userId = TOKEN_USER_MAP.remove(token);
        TOKEN_ROLE_MAP.remove(token);
        if (userId != null) {
            logger.debug("Token removed for user {}", userId);
        }
    }

    /**
     * Get user ID from token
     */
    public Integer getUserIdFromToken(String token) {
        return TOKEN_USER_MAP.get(token);
    }

    /**
     * Get role from token
     */
    public String getRoleFromToken(String token) {
        return TOKEN_ROLE_MAP.get(token);
    }

    /**
     * Validate if request is authenticated
     */
    public boolean isAuthenticated(String authToken, String authStatus) {
        // Backward compatibility with header-based auth
        if (authToken == null && "true".equals(authStatus)) {
            logger.warn("Using legacy authentication header");
            return true;
        }

        if (!devTokenEnabled || authToken == null) {
            return false;
        }

        return TOKEN_USER_MAP.containsKey(authToken);
    }

    /**
     * Validate if user has required role
     */
    public boolean hasRole(String authToken, String... requiredRoles) {
        String userRole = TOKEN_ROLE_MAP.get(authToken);
        if (userRole == null) {
            return false;
        }

        for (String role : requiredRoles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if connection is secure (HTTPS)
     */
    public boolean isSecureConnection(HttpServletRequest request) {
        return request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Protocol"));
    }

    /**
     * Check if HTTPS is required for this operation
     */
    public boolean isHttpsRequired(HttpServletRequest request, boolean isAdminOperation) {
        // If SSL not enabled, don't require HTTPS
        if (!sslEnabled) {
            return false;
        }

        // Check if we're in development and HTTP is allowed
        if (isLocalEnvironment() && allowHttpInDev) {
            return false;
        }

        // Admin operations require HTTPS when enforced
        if (isAdminOperation && enforceHttpsForAdmin) {
            return !isSecureConnection(request);
        }

        return false;
    }

    private boolean isLocalEnvironment() {
        return activeProfile.contains("local") ||
                activeProfile.contains("dev") ||
                activeProfile.contains("default");
    }
}