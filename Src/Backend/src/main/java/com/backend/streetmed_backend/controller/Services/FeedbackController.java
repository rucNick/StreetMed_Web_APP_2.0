package com.backend.streetmed_backend.controller.Services;

import com.backend.streetmed_backend.entity.Service_entity.Feedback;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.service.FeedbackService;
import com.backend.streetmed_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Feedback Management", description = "APIs for submitting and managing feedback")
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final UserService userService;
    private final Executor asyncExecutor;
    private final Executor readOnlyExecutor;

    @Autowired
    public FeedbackController(
            FeedbackService feedbackService,
            UserService userService,
            @Qualifier("authExecutor") Executor asyncExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.asyncExecutor = asyncExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
    }

    @Operation(summary = "Submit feedback",
            description = "Submit feedback without requiring login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback submitted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Feedback submitted successfully",
                    "feedbackId": 1
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/submit")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> submitFeedback(
            @RequestBody @Schema(example = """
                {
                    "name": "John Doe",
                    "phoneNumber": "412-555-0123",
                    "content": "The service was excellent! Very responsive team."
                }
                """) Map<String, String> feedbackData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate required fields
                String name = feedbackData.get("name");
                String content = feedbackData.get("content");

                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException("Name is required");
                }

                if (content == null || content.trim().isEmpty()) {
                    throw new IllegalArgumentException("Feedback content is required");
                }

                Feedback feedback = new Feedback();
                feedback.setName(name);
                feedback.setPhoneNumber(feedbackData.get("phoneNumber"));
                feedback.setContent(content);

                Feedback savedFeedback = feedbackService.submitFeedback(feedback);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Feedback submitted successfully");
                response.put("feedbackId", savedFeedback.getId());

                return ResponseEntity.ok(response);
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, asyncExecutor);
    }

    @Operation(summary = "Get all feedback",
            description = "Get all feedback submissions (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "data": [
                        {
                            "id": 1,
                            "name": "John Doe",
                            "phoneNumber": "412-555-0123",
                            "content": "The service was excellent! Very responsive team.",
                            "createdAt": "2025-02-22T14:30:45"
                        },
                        {
                            "id": 2,
                            "name": "Jane Smith",
                            "phoneNumber": "412-555-0124",
                            "content": "Great experience overall. Would recommend!",
                            "createdAt": "2025-02-22T15:20:10"
                        }
                    ]
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - admin only")
    })
    @GetMapping("/all")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllFeedbacks(
            @RequestHeader(name = "Admin-Username")
            @Parameter(description = "Username of the admin") String adminUsername,
            @RequestHeader(name = "Authentication-Status")
            @Parameter(description = "Authentication status (must be 'true')") String authStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                List<Feedback> feedbacks = feedbackService.getAllFeedbacks();

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", feedbacks);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Delete feedback",
            description = "Delete a feedback submission (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Feedback deleted successfully"
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - admin only")
    })
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteFeedback(
            @Parameter(description = "ID of the feedback to delete")
            @PathVariable Integer id,
            @RequestHeader(name = "Admin-Username")
            @Parameter(description = "Username of the admin") String adminUsername,
            @RequestHeader(name = "Authentication-Status")
            @Parameter(description = "Authentication status (must be 'true')") String authStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                feedbackService.deleteFeedback(id);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Feedback deleted successfully");

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, asyncExecutor);
    }

    @Operation(summary = "Search feedback",
            description = "Search feedback by name (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "data": [
                        {
                            "id": 1,
                            "name": "John Doe",
                            "phoneNumber": "412-555-0123",
                            "content": "The service was excellent! Very responsive team.",
                            "createdAt": "2025-02-22T14:30:45"
                        }
                    ]
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - admin only")
    })
    @GetMapping("/search")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchFeedbacks(
            @Parameter(description = "Name to search for")
            @RequestParam String name,
            @RequestHeader(name = "Admin-Username")
            @Parameter(description = "Username of the admin") String adminUsername,
            @RequestHeader(name = "Authentication-Status")
            @Parameter(description = "Authentication status (must be 'true')") String authStatus) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Unauthorized access");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }

                List<Feedback> feedbacks = feedbackService.searchFeedbacks(name);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", feedbacks);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }


    @Operation(summary = "Mark feedback as read",
            description = "Mark a specific feedback as read (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feedback marked as read"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @PutMapping("/{id}/read")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> markAsRead(
            @Parameter(description = "ID of the feedback")
            @PathVariable Integer id,
            @RequestBody Map<String, String> authData) { // Accepting auth data from Body to match Frontend
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = authData.get("adminUsername");
                String authStatus = authData.get("authenticated");

                // 1. Authenticate
                if (!"true".equals(authStatus)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("status", "error", "message", "Not authenticated"));
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "Unauthorized access"));
                }

                // 2. Perform Action
                Feedback updatedFeedback = feedbackService.markAsRead(id);

                // 3. Response
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Feedback marked as read");
                response.put("data", updatedFeedback);

                return ResponseEntity.ok(response);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", e.getMessage()));
            }
        }, asyncExecutor);
    }
}
