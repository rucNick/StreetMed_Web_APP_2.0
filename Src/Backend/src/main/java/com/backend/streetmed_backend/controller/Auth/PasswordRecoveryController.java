package com.backend.streetmed_backend.controller.Auth;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.UserService;
import com.backend.streetmed_backend.util.ResponseUtil;
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
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final UserService userService;
    private final EmailService emailService;
    private final Executor authExecutor;
    private final TLSService tlsService;
    private final ConcurrentHashMap<String, ResetTokenInfo> resetTokens = new ConcurrentHashMap<>();

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
            @Qualifier("authExecutor") Executor authExecutor,
            TLSService tlsService) {
        this.userService = userService;
        this.emailService = emailService;
        this.authExecutor = authExecutor;
        this.tlsService = tlsService;
        startTokenCleanupTask();
    }

    private void startTokenCleanupTask() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
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

    @Operation(summary = "Request password reset",
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
            @ApiResponse(responseCode = "400", description = "Missing email"),
            @ApiResponse(responseCode = "403", description = "HTTPS required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/request-reset")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> requestPasswordReset(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, false)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Password recovery requires secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String email = request.get("email");

                if (email == null || email.trim().isEmpty()) {
                    return ResponseUtil.badRequest("Email is required");
                }

                User user = userService.findByEmail(email);

                Map<String, Object> response = new HashMap<>();
                response.put("secure", tlsService.isSecureConnection(httpRequest));

                if (user == null) {
                    response.put("message", "If your email is registered, you will receive a recovery code");
                    logger.info("Password reset requested for non-existent email: {}", email);
                    return ResponseUtil.successData(response);
                }

                emailService.sendPasswordRecoveryEmail(email);
                logger.info("Password recovery email sent to user: {}", user.getUserId());

                return ResponseUtil.success("Recovery code sent to your email", response);

            } catch (Exception e) {
                logger.error("Error processing password reset request", e);
                return ResponseUtil.internalError("Failed to process password reset request");
            }
        }, authExecutor);
    }

    @Operation(summary = "Verify OTP",
            description = "Verifies the one-time password (OTP) and returns a reset token if valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Missing required fields or invalid OTP"),
            @ApiResponse(responseCode = "403", description = "HTTPS required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/verify-otp")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyOtp(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, false)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Password recovery requires secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String email = request.get("email");
                String otp = request.get("otp");

                if (email == null || otp == null) {
                    return ResponseUtil.badRequest("Email and OTP are required");
                }

                User user = userService.findByEmail(email);
                if (user == null) {
                    return ResponseUtil.notFound("User not found");
                }

                boolean isValidOtp = emailService.verifyOtp(email, otp);
                if (!isValidOtp) {
                    logger.warn("Invalid OTP attempt for email: {}", email);
                    return ResponseUtil.badRequest("Invalid or expired OTP");
                }

                String resetToken = UUID.randomUUID().toString();
                ResetTokenInfo tokenInfo = new ResetTokenInfo(user.getUserId(), email);
                resetTokens.put(resetToken, tokenInfo);

                logger.info("Reset token generated for user: {} (expires at: {})",
                        user.getUserId(), tokenInfo.expiryTime);

                Map<String, Object> response = new HashMap<>();
                response.put("resetToken", resetToken);
                response.put("userId", user.getUserId());
                response.put("tokenExpiryMinutes", RESET_TOKEN_EXPIRY_MINUTES);
                response.put("secure", tlsService.isSecureConnection(httpRequest));

                return ResponseUtil.success("OTP verified successfully", response);

            } catch (Exception e) {
                logger.error("Error verifying OTP", e);
                return ResponseUtil.internalError("Failed to verify OTP");
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset password",
            description = "Resets the user's password using a valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token"),
            @ApiResponse(responseCode = "403", description = "HTTPS required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> resetPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        if (!tlsService.isSecureConnection(httpRequest)) {
            logger.error("Password reset attempted over insecure connection!");
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Password reset requires secure HTTPS connection"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String resetToken = request.get("resetToken");
                String newPassword = request.get("newPassword");

                if (resetToken == null || newPassword == null) {
                    return ResponseUtil.badRequest("Reset token and new password are required");
                }

                ResetTokenInfo tokenInfo = resetTokens.get(resetToken);
                if (tokenInfo == null) {
                    logger.warn("Invalid reset token attempted");
                    return ResponseUtil.unauthorized("Invalid reset token");
                }

                if (tokenInfo.isExpired()) {
                    logger.warn("Expired reset token attempted for user: {}", tokenInfo.userId);
                    resetTokens.remove(resetToken);
                    return ResponseUtil.unauthorized("Reset token has expired. Please request a new one.");
                }

                if (newPassword.length() < 8) {
                    return ResponseUtil.badRequest("Password must be at least 8 characters long");
                }

                userService.updatePassword(tokenInfo.userId, newPassword);
                logger.info("Password reset successfully for user: {}", tokenInfo.userId);

                resetTokens.remove(resetToken);

                try {
                    emailService.sendPasswordChangeConfirmation(tokenInfo.email);
                } catch (Exception e) {
                    logger.error("Failed to send password change confirmation email", e);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("secure", tlsService.isSecureConnection(httpRequest));

                return ResponseUtil.success("Password reset successfully", response);

            } catch (Exception e) {
                logger.error("Error resetting password", e);
                return ResponseUtil.internalError("Failed to reset password");
            }
        }, authExecutor);
    }

    @Operation(summary = "Get password recovery status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPasswordRecoveryStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        status.put("currentConnectionSecure", tlsService.isSecureConnection(request));
        status.put("httpsRequired", tlsService.isHttpsRequired(request, false));
        status.put("activeTokens", resetTokens.size());
        status.put("tokenExpiryMinutes", RESET_TOKEN_EXPIRY_MINUTES);
        return ResponseEntity.ok(status);
    }
}