package com.backend.streetmed_backend.util;

import com.backend.streetmed_backend.security.SecurityManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for handling encrypted request/response operations
 * Centralizes encryption/decryption logic used across controllers
 */
@Component
public class RequestResponseUtil {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseUtil.class);

    private final SecurityManager securityManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public RequestResponseUtil(SecurityManager securityManager, ObjectMapper objectMapper) {
        this.securityManager = securityManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses the request body with encryption support
     *
     * @param sessionId The session ID for encryption
     * @param body The request body
     * @param logPrefix Prefix for logging
     * @return Parsed map of request data
     * @throws Exception if parsing fails
     */
    public Map<String, String> parseRequestBody(String sessionId, String body, String logPrefix) throws Exception {
        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                logger.info("Received encrypted {} request for session: {}", logPrefix, sessionId);
                String decryptedBody = securityManager.decrypt(sessionId, body);
                Map<String, String> data = objectMapper.readValue(decryptedBody, Map.class);
                logger.info("Decrypted {} request", logPrefix);
                return data;
            } catch (IllegalStateException e) {
                logger.warn("Session key not found for {}: {}, trying to parse as regular JSON", logPrefix, e.getMessage());
                try {
                    return objectMapper.readValue(body, Map.class);
                } catch (Exception jsonEx) {
                    throw new RuntimeException("Failed to parse request as JSON after decryption failed for " + logPrefix, jsonEx);
                }
            }
        } else {
            return objectMapper.readValue(body, Map.class);
        }
    }

    /**
     * Builds a ResponseEntity with encryption support
     *
     * @param sessionId The session ID for encryption
     * @param response The response object to potentially encrypt
     * @param status The HTTP status code
     * @return ResponseEntity with encrypted or plain response
     */
    public ResponseEntity<?> buildResponse(String sessionId, Object response, HttpStatus status) {
        if (sessionId != null && !sessionId.isEmpty() && securityManager.getSessionKey(sessionId) != null) {
            try {
                String encryptedResponse = securityManager.encrypt(sessionId, objectMapper.writeValueAsString(response));
                return ResponseEntity.status(status).body(encryptedResponse);
            } catch (Exception e) {
                logger.error("Error encrypting response: {}", e.getMessage());
            }
        }
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Builds a ResponseEntity from an existing ResponseEntity with encryption support
     *
     * @param sessionId The session ID for encryption
     * @param responseEntity The ResponseEntity to potentially encrypt
     * @return ResponseEntity with encrypted or plain response
     */
    public ResponseEntity<?> buildEncryptedResponse(String sessionId, ResponseEntity<Map<String, Object>> responseEntity) {
        if (sessionId != null && !sessionId.isEmpty() && securityManager.getSessionKey(sessionId) != null) {
            try {
                String encryptedResponse = securityManager.encrypt(sessionId,
                        objectMapper.writeValueAsString(responseEntity.getBody()));
                return ResponseEntity.status(responseEntity.getStatusCode()).body(encryptedResponse);
            } catch (Exception e) {
                logger.error("Error encrypting response: {}", e.getMessage());
            }
        }
        return responseEntity;
    }


}