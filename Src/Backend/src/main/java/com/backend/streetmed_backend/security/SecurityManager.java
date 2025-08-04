package com.backend.streetmed_backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private final ECDHService ecdhService;
    private final EncryptionUtil encryptionUtil;

    // Store derived session keys
    private final Map<String, SecretKey> sessionKeys = new ConcurrentHashMap<>();

    // Map to track when sessions were created (or last used)
    private final Map<String, Long> sessionTimestamps = new ConcurrentHashMap<>();

    @Autowired
    public SecurityManager(ECDHService ecdhService, EncryptionUtil encryptionUtil) {
        this.ecdhService = ecdhService;
        this.encryptionUtil = encryptionUtil;
        logger.info("SecurityManager initialized");
    }

    /**
     * Gets the secret key for the session.
     */
    public SecretKey getSessionKey(String sessionId) {
        return sessionKeys.get(sessionId);
    }

    /**
     * Completes the handshake and derives a session key.
     * Also records the session creation time.
     */
    public void completeHandshake(String sessionId, String clientPublicKey) {
        try {
            String sharedSecret = ecdhService.computeSharedSecret(sessionId, clientPublicKey);
            SecretKey key = encryptionUtil.deriveKey(sharedSecret);
            sessionKeys.put(sessionId, key);
            // Record the session creation time
            sessionTimestamps.put(sessionId, System.currentTimeMillis());
            logger.info("Handshake completed and session key derived for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Failed to complete handshake: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Encrypts data for a specific session.
     */
    public String encrypt(String sessionId, String data) {
        SecretKey key = sessionKeys.get(sessionId);
        if (key == null) {
            logger.error("No key found for session: {}", sessionId);
            throw new IllegalStateException("Session key not found: " + sessionId);
        }
        return encryptionUtil.encrypt(data, key);
    }

    /**
     * Decrypts data for a specific session.
     */
    public String decrypt(String sessionId, String encryptedData) {
        SecretKey key = sessionKeys.get(sessionId);
        if (key == null) {
            logger.error("No key found for session: {}", sessionId);
            throw new IllegalStateException("Session key not found: " + sessionId);
        }
        return encryptionUtil.decrypt(encryptedData, key);
    }

    /**
     * Removes all data associated with a session.
     */
    public void removeSession(String sessionId) {
        sessionKeys.remove(sessionId);
        sessionTimestamps.remove(sessionId);
        ecdhService.removeKeyPair(sessionId);
        logger.info("Session data removed for session: {}", sessionId);
    }

    /**
     * Scheduled cleanup task that runs every 5 minutes
     * to remove sessions that have exceeded the timeout.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        for (String sessionId : sessionTimestamps.keySet()) {
            Long timestamp = sessionTimestamps.get(sessionId);
            // Set session timeout to 30 minutes (in milliseconds)
            // 30 minutes
            long SESSION_TIMEOUT_MILLIS = 30 * 60 * 1000;
            if (timestamp != null && (currentTime - timestamp) > SESSION_TIMEOUT_MILLIS) {
                logger.info("Session {} expired, cleaning up", sessionId);
                removeSession(sessionId);
            }
        }
    }
}
