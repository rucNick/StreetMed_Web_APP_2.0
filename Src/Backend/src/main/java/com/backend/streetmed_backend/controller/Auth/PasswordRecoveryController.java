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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private final UserService userService;
    private final EmailService emailService;
    private final Executor authExecutor;

    // Store reset tokens with user IDs
    private final ConcurrentHashMap<String, Integer> resetTokens = new ConcurrentHashMap<>();

    @Autowired
    public PasswordRecoveryController(
            UserService userService,
            EmailService emailService,
            @Qualifier("authExecutor") Executor authExecutor) {
        this.userService = userService;
        this.emailService = emailService;
        this.authExecutor = authExecutor;
    }

    @Operation(summary = "Request password reset",
            description = "Sends a password recovery code to the user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recovery code sent successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Recovery code sent to your email"
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
            Map<String, String> request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String email = request.get("email");

                if (email == null || email.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Email is required"
                    ));
                }

                User user = userService.findByEmail(email);
                if (user == null) {
                    // Don't reveal that the email doesn't exist for security reasons
                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "If your email is registered, you will receive a recovery code"
                    ));
                }

                emailService.sendPasswordRecoveryEmail(email);

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Recovery code sent to your email"
                ));

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Verify OTP",
            description = "Verifies the one-time password (OTP) and returns a reset token if valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP verified successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "OTP verified successfully",
                    "resetToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                    "userId": 123
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
            Map<String, String> request) {

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
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired OTP");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // Generate a reset token and store it with user ID
                String resetToken = UUID.randomUUID().toString();
                resetTokens.put(resetToken, user.getUserId());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "OTP verified successfully");
                response.put("resetToken", resetToken);
                response.put("userId", user.getUserId());

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Reset password",
            description = "Resets the user's password using a valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Password reset successfully"
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
            Map<String, String> request) {

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
                Integer userId = resetTokens.get(resetToken);
                if (userId == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Invalid or expired reset token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                // Update password
                userService.updatePassword(userId, newPassword);

                // Remove the used token
                resetTokens.remove(resetToken);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Password reset successfully");
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }
}