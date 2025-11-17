package com.backend.streetmed_backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Manager that supports both legacy ECDH encryption and new TLS-only mode
 * This allows gradual migration from custom encryption to TLS
 */
@Component
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private final ECDHService ecdhService;
    private final EncryptionUtil encryptionUtil;

    // Store derived session keys
    private final Map<String, SecretKey> sessionKeys = new ConcurrentHashMap<>();

    // Map to track when sessions were created (or last used)
    private final Map<String, Long> sessionTimestamps = new ConcurrentHashMap<>();

    // Session attributes storage
    private final Map<String, Map<String, Object>> sessionAttributes = new ConcurrentHashMap<>();

    // Configuration for migration
    @Value("${security.use.custom.encryption:true}")
    private boolean useCustomEncryption;

    @Value("${server.ssl.enabled:false}")
    private boolean tlsEnabled;

    private static final long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30 minutes

    @Autowired
    public SecurityManager(ECDHService ecdhService, EncryptionUtil encryptionUtil) {
        this.ecdhService = ecdhService;
        this.encryptionUtil = encryptionUtil;
        logger.info("SecurityManager initialized - Custom Encryption: {}, TLS: {}",
                useCustomEncryption, tlsEnabled);
    }

    /**
     * Check if session is ready for encryption
     */
    public boolean isSessionReady(String sessionId) {
        if (!useCustomEncryption) {
            // In TLS-only mode, sessions are always "ready"
            return true;
        }
        return sessionKeys.containsKey(sessionId);
    }

    /**
     * Gets the secret key for the session.
     */
    public SecretKey getSessionKey(String sessionId) {
        if (!useCustomEncryption) {
            return null; // No key needed in TLS-only mode
        }
        return sessionKeys.get(sessionId);
    }

    /**
     * Completes the handshake and derives a session key.
     * In TLS-only mode, this is a no-op but maintains compatibility
     */
    public void completeHandshake(String sessionId, String clientPublicKey) {
        if (!useCustomEncryption) {
            logger.debug("Skipping handshake in TLS-only mode for session: {}", sessionId);
            sessionTimestamps.put(sessionId, System.currentTimeMillis());
            return;
        }

        try {
            String sharedSecret = ecdhService.computeSharedSecret(sessionId, clientPublicKey);
            SecretKey key = encryptionUtil.deriveKey(sharedSecret);
            sessionKeys.put(sessionId, key);
            sessionTimestamps.put(sessionId, System.currentTimeMillis());
            logger.info("Handshake completed and session key derived for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Failed to complete handshake: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Encrypts data for a specific session.
     * In TLS-only mode, returns data as-is (TLS handles encryption)
     */
    public String encrypt(String sessionId, String data) {
        if (!useCustomEncryption) {
            logger.debug("TLS-only mode: returning unencrypted data (TLS will handle encryption)");
            return data;
        }

        SecretKey key = sessionKeys.get(sessionId);
        if (key == null) {
            logger.error("No key found for session: {}", sessionId);
            throw new IllegalStateException("Session key not found: " + sessionId);
        }
        return encryptionUtil.encrypt(data, key);
    }

    /**
     * Decrypts data for a specific session.
     * In TLS-only mode, returns data as-is (TLS handles decryption)
     */
    public String decrypt(String sessionId, String encryptedData) {
        if (!useCustomEncryption) {
            logger.debug("TLS-only mode: returning data as-is (TLS handled decryption)");
            return encryptedData;
        }

        SecretKey key = sessionKeys.get(sessionId);
        if (key == null) {
            logger.error("No key found for session: {}", sessionId);
            throw new IllegalStateException("Session key not found: " + sessionId);
        }
        return encryptionUtil.decrypt(encryptedData, key);
    }

    /**
     * Store session attribute
     */
    public void setSessionAttribute(String sessionId, String key, Object value) {
        sessionAttributes.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(key, value);
        // Update last access time
        sessionTimestamps.put(sessionId, System.currentTimeMillis());
    }

    /**
     * Get session attribute
     */
    public Object getSessionAttribute(String sessionId, String key) {
        Map<String, Object> attrs = sessionAttributes.get(sessionId);
        return attrs != null ? attrs.get(key) : null;
    }

    /**
     * Removes all data associated with a session.
     */
    public void removeSession(String sessionId) {
        sessionKeys.remove(sessionId);
        sessionTimestamps.remove(sessionId);
        sessionAttributes.remove(sessionId);
        if (useCustomEncryption) {
            ecdhService.removeKeyPair(sessionId);
        }
        logger.info("Session data removed for session: {}", sessionId);
    }

    /**
     * Check if we should use custom encryption for a specific endpoint
     * This allows gradual migration
     */
    public boolean shouldUseCustomEncryption(String endpoint) {
        // You can configure which endpoints still use custom encryption
        if (!useCustomEncryption) {
            return false;
        }

        // Example: Only use custom encryption for specific endpoints during migration
        // This allows you to migrate endpoint by endpoint
        if (endpoint != null) {
            // Add endpoints that should still use custom encryption
            return endpoint.contains("/auth/login") ||
                    endpoint.contains("/auth/register");
        }

        return useCustomEncryption;
    }

    /**
     * Get migration status for monitoring
     */
    public Map<String, Object> getMigrationStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("customEncryptionEnabled", useCustomEncryption);
        status.put("tlsEnabled", tlsEnabled);
        status.put("activeSessions", sessionKeys.size());
        status.put("mode", useCustomEncryption ? "HYBRID" : "TLS_ONLY");
        return status;
    }

    /**
     * Scheduled cleanup task that runs every 5 minutes
     * to remove sessions that have exceeded the timeout.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        int cleanedCount = 0;

        for (Map.Entry<String, Long> entry : sessionTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > SESSION_TIMEOUT_MILLIS) {
                removeSession(entry.getKey());
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            logger.info("Cleaned up {} expired sessions", cleanedCount);
        }
    }
}