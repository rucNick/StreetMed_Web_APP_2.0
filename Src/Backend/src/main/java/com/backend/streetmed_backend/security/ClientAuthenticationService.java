package com.backend.streetmed_backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientAuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthenticationService.class);

    // Map to store registered clients with their shared secrets
    private final Map<String, String> clientSecrets = new ConcurrentHashMap<>();

    // Map to track which client initiated which session
    private final Map<String, String> sessionClients = new ConcurrentHashMap<>();

    // Timestamp tolerance in milliseconds (5 minutes)
    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000;

    @Value("${security.client.authentication.key:street-med-client-authentication-key}")
    private String clientAuthenticationKey;

    public ClientAuthenticationService() {
        // Register known clients
        registerClient("street-med-frontend-local", "local-development-secret");
        registerClient("street-med-frontend-prod", "production-client-secret");
        registerClient("default-client-id", "default-client-secret");
    }

    /**
     * Register a new client with its secret
     */
    public void registerClient(String clientId, String clientSecret) {
        clientSecrets.put(clientId, clientSecret);
        logger.info("Registered client: {}", clientId);
    }

    /**
     * Associate a session ID with a client ID
     */
    public void associateClientWithSession(String sessionId, String clientId) {
        sessionClients.put(sessionId, clientId);
        logger.info("Associated session {} with client {}", sessionId, clientId);
    }

    /**
     * Validate if the client ID for a session matches
     */
    public boolean validateSessionClient(String sessionId, String clientId) {
        String associatedClientId = sessionClients.get(sessionId);

        // If no client is associated yet, allow it (first request)
        if (associatedClientId == null) {
            return true;
        }

        return associatedClientId.equals(clientId);
    }

    /**
     * Validate a client request by checking:
     * 1. Client ID is registered
     * 2. Timestamp is within tolerance
     * 3. Signature is valid
     */
    public boolean validateClientRequest(String clientId, String timestamp, String signature) {
        // For development purposes, allow requests without authentication
        // Remove or modify this in production
        if (isDevEnvironment()) {
            logger.info("Development environment: skipping client authentication for client {}", clientId);
            return true;
        }

        // Check if client ID is provided and registered
        if (clientId == null || !clientSecrets.containsKey(clientId)) {
            logger.warn("Unknown client ID: {}", clientId);
            return false;
        }

        // Very relaxed validation for development - just check clientId
        // In production, implement full validation with timestamp and signature
        if (isRelaxedValidation()) {
            logger.info("Relaxed validation: accepting client {}", clientId);
            return true;
        }

        // Check timestamp
        if (timestamp == null || !isTimestampValid(timestamp)) {
            logger.warn("Invalid timestamp for client {}: {}", clientId, timestamp);
            return false;
        }

        // Check signature
        if (signature == null || !isSignatureValid(clientId, timestamp, signature)) {
            logger.warn("Invalid signature for client {}", clientId);
            return false;
        }

        return true;
    }

    /**
     * Check if the timestamp is within tolerance
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long difference = Math.abs(currentTime - requestTimestamp);

            return difference <= TIMESTAMP_TOLERANCE_MS;
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    /**
     * Validate the client's signature
     */
    private boolean isSignatureValid(String clientId, String timestamp, String signature) {
        try {
            // In a real implementation, you would use the client's secret
            // For now, we'll use a shared key for all clients
            String data = clientId + ":" + timestamp;

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    clientAuthenticationKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error validating signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if we're in development environment
     */
    private boolean isDevEnvironment() {
        // Get environment from application properties or environment variable
        String environment = System.getProperty("spring.profiles.active", "development");
        return "development".equals(environment) || "dev".equals(environment);
    }

    /**
     * Check if we should use relaxed validation
     */
    private boolean isRelaxedValidation() {
        // Get property from application properties
        String relaxed = System.getProperty("security.client.validation.relaxed", "true");
        return "true".equals(relaxed);
    }
}