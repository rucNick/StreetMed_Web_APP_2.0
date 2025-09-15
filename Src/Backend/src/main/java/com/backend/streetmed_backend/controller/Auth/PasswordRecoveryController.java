package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Password Recovery", description = "APIs for password recovery and reset")
@RestController
@RequestMapping("/api/auth/password")
public class PasswordRecoveryController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryController.class);

    private final UserService userService;
    private final EmailService emailService;
    private final Executor authExecutor;

    // Store reset tokens with user IDs and expiration time
    private final ConcurrentHashMap<String, ResetTokenInfo> resetTokens = new ConcurrentHashMap<>();

    // Token expiration time in minutes
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    // TLS configuration
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${tls.enforce.password.recovery:true}")
    private boolean enforceHttpsForPasswordRecovery;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${tls.allow.http.in.dev:false}")
    private boolean allowHttpInDev;

    // Inner class to store token info with expiration
    private static class ResetTokenInfo {
        public final Integer userId;
        public final LocalDateTime expiryTime;
        public final String email;

        public ResetTokenInfo(Integer userId, String email) {
            this.userId = userId;
            this.email = email;
            this.expiryTime = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES);
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    @Autowired
    public PasswordRecoveryController(
            UserService userService,
            EmailService emailService,
            @Qualifier("authExecutor") Executor authExecutor) {
        this.userService = userService;
        this.emailService = emailService;
        this.authExecutor = authExecutor;
        logger.info("PasswordRecoveryController initialized - SSL: {}, Enforce HTTPS: {}",
                sslEnabled, enforceHttpsForPasswordRecovery);

        // Start cleanup task for expired tokens
        startTokenCleanupTask();
    }

    /**
     * Check if connection is secure (HTTPS)
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        boolean isSecure = request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Protocol"));

        logger.debug("Password recovery connection check - Scheme: {}, Secure: {}, Port: {}",
                request.getScheme(),
                request.isSecure(),
                request.getServerPort());

        return isSecure;
    }

    /**
     * Check if the connection meets security requirements
     * Password recovery should ALWAYS use HTTPS in production
     */
    private boolean isConnectionAllowed(HttpServletRequest request) {
        boolean isSecure = isSecureConnection(request);

        // Password recovery contains sensitive data - enforce HTTPS when SSL is enabled
        if (sslEnabled && enforceHttpsForPasswordRecovery) {
            if (!isSecure) {
                logger.warn("Insecure password recovery request blocked from: {} - Endpoint: {}",
                        request.getRemoteAddr(), request.getRequestURI());
                return false;
            }
        }

        // In development, optionally allow HTTP (NOT recommended for password operations)
        if (isLocalEnvironment() && allowHttpInDev) {
            if (!isSecure) {
                logger.warn("⚠️ PASSWORD RECOVERY OVER HTTP IN DEV MODE - THIS IS INSECURE!");
            }
            return true;
        }

        // For production or when HTTP is not explicitly allowed, require HTTPS
        if (!isSecure && sslEnabled) {
            logger.error("HTTP request blocked for password recovery when HTTPS is available");
            return false;
        }

        return true;
    }

    /**
     * Check if running in local environment
     */
    private boolean isLocalEnvironment() {
        return activeProfile.contains("local") ||
                activeProfile.contains("dev") ||
                activeProfile.contains("default");
    }

    /**
     * Standard HTTPS error response
     */
    private ResponseEntity<Map<String, Object>> createHttpsRequiredResponse() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "Password recovery requires secure HTTPS connection");
        errorResponse.put("httpsRequired", true);
        errorResponse.put("sslEnabled", sslEnabled);
        errorResponse.put("hint", sslEnabled ? "Use HTTPS on port 8443" : "SSL is not enabled on server");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Start background task to clean up expired tokens
     */
    private void startTokenCleanupTask() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    int removed = 0;
                    for (Map.Entry<String, ResetTokenInfo> entry : resetTokens.entrySet()) {
                        if (entry.getValue().isExpired()) {
                            resetTokens.remove(entry.getKey());
                            removed++;
                        }
                    }
                    if (removed > 0) {
                        logger.info("Cleaned up {} expired reset tokens", removed);
                    }
                } catch (InterruptedException e) {
                    logger.error("Token cleanup task interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    @Operation(summary = "Request password reset (HTTPS required)",
            description = "Sends a password recovery code to the user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recovery code sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Recovery code sent to your email",
                    "secure": true
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing email",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Email is required"
                }
                """))),
            @ApiResponse(responseCode = "403", description = "HTTPS required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Password recovery requires secure HTTPS connection",
                    "httpsRequired": true
                }
                """))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Error details"
                }
                """)))
    })
    @PostMapping("/request-reset")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> requestPasswordReset(
            @RequestBody @Schema(description = "Email request object",
                    required = true,
                    example = """
                {
                    "email": "user@example.com"
                }
                """)
            Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Check HTTPS requirement
        if (!isConnectionAllowed(httpRequest)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String email = request.get("email");

                if (email == null || email.trim().isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Email is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                User user = userService.findByEmail(email);

                // Always return success for security (don't reveal if email exists)
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("secure", isSecureConnection(httpRequest));

                if (user == null) {
                    // Don't reveal that the email doesn't exist
                    response.put("message", "If your email is registered, you will receive a recovery code");
                    logger.info("Password reset requested for non-existent email: {}", email);
                    return ResponseEntity.ok(response);
                }

                // Send recovery email
                emailService.sendPasswordRecoveryEmail(email);
                logger.info("Password recovery email sent to user: {}", user.getUserId());

                response.put("message", "Recovery code sent to your email");
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error processing password reset request", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Failed to process password reset request");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Verify OTP (HTTPS required)",
            description = "Verifies the one-time password (OTP) and returns a reset token if valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP verified successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "OTP verified successfully",
                    "resetToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                    "userId": 123,
                    "tokenExpiryMinutes": 30,
                    "secure": true
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields or invalid OTP",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Email and OTP are required"
                }
                """))),
            @ApiResponse(responseCode = "403", description = "HTTPS required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Password recovery requires secure HTTPS connection",
                    "httpsRequired": true
                }
                """))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "User not found"
                }
                """))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Error details"
                }
                """)))
    })
    @PostMapping("/verify-otp")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyOtp(
            @RequestBody @Schema(description = "OTP verification request",
                    required = true,
                    example = """
                {
                    "email": "user@example.com",
                    "otp": "123456"
                }
                """)
            Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Check HTTPS requirement
        if (!isConnectionAllowed(httpRequest)) {
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String email = request.get("email");
                String otp = request.get("otp");

                if (email == null || otp == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Email and OTP are required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                User user = userService.findByEmail(email);
                if (user == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                boolean isValidOtp = emailService.verifyOtp(email, otp);
                if (!isValidOtp) {
                    logger.warn("Invalid OTP attempt for email: {}", email);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired OTP");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // Generate a reset token and store it with user ID and expiration
                String resetToken = UUID.randomUUID().toString();
                ResetTokenInfo tokenInfo = new ResetTokenInfo(user.getUserId(), email);
                resetTokens.put(resetToken, tokenInfo);

                logger.info("Reset token generated for user: {} (expires at: {})",
                        user.getUserId(), tokenInfo.expiryTime);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "OTP verified successfully");
                response.put("resetToken", resetToken);
                response.put("userId", user.getUserId());
                response.put("tokenExpiryMinutes", RESET_TOKEN_EXPIRY_MINUTES);
                response.put("secure", isSecureConnection(httpRequest));

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error verifying OTP", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Failed to verify OTP");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset password (HTTPS required)",
            description = "Resets the user's password using a valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Password reset successfully",
                    "secure": true
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Reset token and new password are required"
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Invalid or expired reset token"
                }
                """))),
            @ApiResponse(responseCode = "403", description = "HTTPS required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Password recovery requires secure HTTPS connection",
                    "httpsRequired": true
                }
                """))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "error",
                    "message": "Error details"
                }
                """)))
    })
    @PostMapping("/reset")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @RequestBody @Schema(description = "Password reset request",
                    required = true,
                    example = """
                {
                    "resetToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                    "newPassword": "NewSecurePassword123"
                }
                """)
            Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Password reset MUST use HTTPS when SSL is enabled
        if (sslEnabled && !isSecureConnection(httpRequest)) {
            logger.error("Password reset attempted over insecure connection!");
            return CompletableFuture.completedFuture(createHttpsRequiredResponse());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String resetToken = request.get("resetToken");
                String newPassword = request.get("newPassword");

                if (resetToken == null || newPassword == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Reset token and new password are required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // Validate the reset token
                ResetTokenInfo tokenInfo = resetTokens.get(resetToken);
                if (tokenInfo == null) {
                    logger.warn("Invalid reset token attempted");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid reset token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Check if token is expired
                if (tokenInfo.isExpired()) {
                    logger.warn("Expired reset token attempted for user: {}", tokenInfo.userId);
                    resetTokens.remove(resetToken); // Clean up expired token
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Reset token has expired. Please request a new one.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Validate password strength (basic check)
                if (newPassword.length() < 8) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Password must be at least 8 characters long");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // Update password
                userService.updatePassword(tokenInfo.userId, newPassword);
                logger.info("Password reset successfully for user: {}", tokenInfo.userId);

                // Remove the used token
                resetTokens.remove(resetToken);

                // Optionally, send confirmation email
                try {
                    emailService.sendPasswordChangeConfirmation(tokenInfo.email);
                } catch (Exception e) {
                    logger.error("Failed to send password change confirmation email", e);
                    // Don't fail the operation if email fails
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Password reset successfully");
                response.put("secure", isSecureConnection(httpRequest));

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error resetting password", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Failed to reset password");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Get password recovery status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPasswordRecoveryStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        status.put("sslEnabled", sslEnabled);
        status.put("enforceHttps", enforceHttpsForPasswordRecovery);
        status.put("currentConnectionSecure", isSecureConnection(request));
        status.put("connectionAllowed", isConnectionAllowed(request));
        status.put("activeTokens", resetTokens.size());
        status.put("tokenExpiryMinutes", RESET_TOKEN_EXPIRY_MINUTES);
        status.put("environment", activeProfile);

        return ResponseEntity.ok(status);
    }
}