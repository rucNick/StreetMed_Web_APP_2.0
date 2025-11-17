package com.backend.streetmed_backend.controller.Security;

import com.backend.streetmed_backend.security.ClientAuthenticationService;
import com.backend.streetmed_backend.security.ECDHService;
import com.backend.streetmed_backend.security.SecurityManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Security", description = "APIs for security operations including key exchange")
@RestController
@RequestMapping("/api/security")
@CrossOrigin
public class ECDHController {

    @RequestMapping(method = RequestMethod.OPTIONS, path = "/**")
    public ResponseEntity<?> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }

    private static final Logger logger = LoggerFactory.getLogger(ECDHController.class);

    private final ECDHService ecdhService;
    private final SecurityManager securityManager;
    private final ClientAuthenticationService clientAuthService;

    @Autowired
    public ECDHController(ECDHService ecdhService, SecurityManager securityManager,
                          ClientAuthenticationService clientAuthService) {
        this.ecdhService = ecdhService;
        this.securityManager = securityManager;
        this.clientAuthService = clientAuthService;
        logger.info("ECDHController initialized");
    }

    @Operation(summary = "Initiate ECDH handshake",
            description = "Initiates the ECDH key exchange by generating a server key pair and returning the public key")
    @GetMapping("/initiate-handshake")
    public ResponseEntity<?> initiateHandshake(
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        logger.info("Received request to initiate ECDH handshake from client: {}", clientId);

        // Validate client authentication
        if (!clientAuthService.validateClientRequest(clientId, timestamp, signature)) {
            logger.warn("Client authentication failed for client: {}", clientId);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        logger.info("Client authentication successful for client: {}", clientId);

        String sessionId = UUID.randomUUID().toString();
        logger.info("Generated new session ID: {}", sessionId);

        logger.info("Generating server key pair for session: {}", sessionId);
        String serverPublicKey = ecdhService.generateKeyPair(sessionId);
        logger.info("Server key pair generated successfully for session: {}", sessionId);
        logger.debug("Server public key (base64): {}", serverPublicKey);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("serverPublicKey", serverPublicKey);

        // Store client ID associated with this session
        clientAuthService.associateClientWithSession(sessionId, clientId);

        logger.info("ECDH handshake initiated successfully for session: {}", sessionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Complete ECDH handshake",
            description = "Completes the ECDH key exchange by computing the shared secret using the client's public key")
    @PostMapping("/complete-handshake")
    public ResponseEntity<?> completeHandshake(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        String sessionId = request.get("sessionId");
        String clientPublicKey = request.get("clientPublicKey");

        logger.info("Received request to complete ECDH handshake for session: {}", sessionId);
        logger.debug("Request parameters: sessionId={}, clientPublicKeyLength={}",
                sessionId, clientPublicKey != null ? clientPublicKey.length() : "null");

        // Validate client authentication
        if (!clientAuthService.validateClientRequest(clientId, timestamp, signature)) {
            logger.warn("Client authentication failed for client: {}", clientId);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // Verify that the client ID matches the one associated with this session
        if (!clientAuthService.validateSessionClient(sessionId, clientId)) {
            logger.warn("Client ID mismatch for session: {}", sessionId);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Client ID mismatch");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        logger.info("Client authentication successful for session: {}", sessionId);

        if (sessionId == null || clientPublicKey == null) {
            logger.warn("Missing required parameters for session: {}", sessionId);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Missing required parameters");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (!ecdhService.hasKeyPair(sessionId)) {
            logger.warn("Invalid or expired session: {}", sessionId);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid or expired session");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            logger.info("Computing shared secret and deriving session key for session: {}", sessionId);
            securityManager.completeHandshake(sessionId, clientPublicKey);
            logger.info("Handshake completed successfully for session: {}", sessionId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Handshake completed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error completing handshake for session {}: {}", sessionId, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error completing handshake: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}