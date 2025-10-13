package com.backend.streetmed_backend.controller.Services;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerApplication;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.volunteerService.VolunteerApplicationService;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Volunteer Application Management", description = "APIs for managing volunteer applications")
@RestController
@RequestMapping("/api/volunteer")
public class VolunteerApplicationController {
    private final VolunteerApplicationService volunteerApplicationService;
    private final UserService userService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;
    private static final String INITIAL_PASSWORD = "streetmed@pitt";
    private final EmailService emailService;

    @Autowired
    public VolunteerApplicationController(
            VolunteerApplicationService volunteerApplicationService,
            EmailService emailService,
            UserService userService,
            @Qualifier("authExecutor") Executor authExecutor,
            @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.volunteerApplicationService = volunteerApplicationService;
        this.emailService = emailService;
        this.userService = userService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
    }

    @Operation(summary = "Submit a volunteer application",
            description = "Allows a user to submit an application to become a volunteer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application submitted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Application submitted successfully",
                    "applicationId": 1
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "409", description = "Application already exists for this email")
    })

    @PostMapping("/apply")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> submitApplication(
            @RequestBody @Schema(example = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@example.com",
                    "phone": "412-555-0123",
                    "notes": "Available weekends"
                }
                """) Map<String, String> applicationData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate required fields
                if (applicationData.get("firstName") == null || applicationData.get("lastName") == null ||
                        applicationData.get("email") == null || applicationData.get("phone") == null) {
                    throw new RuntimeException("Missing required fields");
                }

                // Check if application already exists
                if (volunteerApplicationService.existsByEmail(applicationData.get("email"))) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "An application with this email already exists");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }

                VolunteerApplication application = new VolunteerApplication(
                        applicationData.get("firstName"),
                        applicationData.get("lastName"),
                        applicationData.get("email"),
                        applicationData.get("phone")
                );

                // Set notes if provided in the application
                if (applicationData.containsKey("notes") && applicationData.get("notes") != null) {
                    application.setNotes(applicationData.get("notes"));
                }

                VolunteerApplication savedApplication = volunteerApplicationService.submitApplication(application);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Application submitted successfully");
                response.put("applicationId", savedApplication.getApplicationId());

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
        }, authExecutor);
    }


    @Operation(summary = "Get all volunteer applications",
            description = "Returns all volunteer applications grouped by status. Admin access only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "data": {
                        "pending": [
                            {
                                "applicationId": 1,
                                "firstName": "John",
                                "lastName": "Doe",
                                "email": "john@example.com",
                                "phone": "412-555-0123",
                                "status": "PENDING",
                                "notes": "Available weekends",
                                "submissionDate": "2024-02-19T10:30:00"
                            }
                        ],
                        "approved": [],
                        "rejected": []
                    }
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - Admin only")
    })
    @GetMapping("/applications")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllApplications(
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

                List<VolunteerApplication> applications = volunteerApplicationService.getAllApplications();
                Map<String, List<Map<String, Object>>> groupedApplications = new HashMap<>();
                groupedApplications.put("pending", new ArrayList<>());
                groupedApplications.put("approved", new ArrayList<>());
                groupedApplications.put("rejected", new ArrayList<>());

                for (VolunteerApplication app : applications) {
                    Map<String, Object> appInfo = new HashMap<>();
                    appInfo.put("applicationId", app.getApplicationId());
                    appInfo.put("firstName", app.getFirstName());
                    appInfo.put("lastName", app.getLastName());
                    appInfo.put("email", app.getEmail());
                    appInfo.put("phone", app.getPhone());
                    appInfo.put("status", app.getStatus());
                    appInfo.put("notes", app.getNotes());
                    appInfo.put("submissionDate", app.getSubmissionDate());

                    switch (app.getStatus()) {
                        case PENDING -> groupedApplications.get("pending").add(appInfo);
                        case APPROVED -> groupedApplications.get("approved").add(appInfo);
                        case REJECTED -> groupedApplications.get("rejected").add(appInfo);
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", groupedApplications);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }


    @Operation(summary = "Get pending volunteer applications",
            description = "Returns only pending volunteer applications. Admin access only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending applications retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "data": [
                        {
                            "applicationId": 1,
                            "firstName": "John",
                            "lastName": "Doe",
                            "email": "john@example.com",
                            "phone": "412-555-0123",
                            "notes": "Available weekends",
                            "submissionDate": "2024-02-19T10:30:00"
                        }
                    ]
                }
                """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - Admin only")
    })
    @GetMapping("/pending")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPendingApplications(
            @RequestHeader(name = "Admin-Username")
            @Parameter(description = "Username of the admin") String adminUsername,
            @RequestHeader(name = "Authentication-Status")
            @Parameter(description = "Authentication status (must be 'true')") String authStatus){
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    throw new RuntimeException("Unauthorized access");
                }

                List<VolunteerApplication> pendingApplications = volunteerApplicationService.getPendingApplications();
                List<Map<String, Object>> applicationList = new ArrayList<>();

                for (VolunteerApplication app : pendingApplications) {
                    Map<String, Object> appInfo = new HashMap<>();
                    appInfo.put("applicationId", app.getApplicationId());
                    appInfo.put("firstName", app.getFirstName());
                    appInfo.put("lastName", app.getLastName());
                    appInfo.put("email", app.getEmail());
                    appInfo.put("phone", app.getPhone());
                    appInfo.put("notes", app.getNotes());
                    appInfo.put("submissionDate", app.getSubmissionDate());
                    applicationList.add(appInfo);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("data", applicationList);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Check volunteer application status",
            description = "Checks the status of a volunteer application based on the applicant's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "applicationId": 1,
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@example.com",
                    "phone": "412-555-0123",
                    "applicationStatus": "PENDING",
                    "notes": "Available weekends",
                    "submissionDate": "2024-02-19T10:30:00"
                }
                """))),
            @ApiResponse(responseCode = "404", description = "No application found for this email")
    })
    @GetMapping("/application/status/{email}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> checkApplicationStatus(
            @Parameter(description = "Email address of the applicant")
            @PathVariable String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<VolunteerApplication> applicationOpt = volunteerApplicationService.findByEmail(email);

                if (applicationOpt.isEmpty()) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "No application found for this email");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                }

                VolunteerApplication application = applicationOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("applicationId", application.getApplicationId());
                response.put("firstName", application.getFirstName());
                response.put("lastName", application.getLastName());
                response.put("email", application.getEmail());
                response.put("phone", application.getPhone());
                response.put("applicationStatus", application.getStatus());
                response.put("notes", application.getNotes());
                response.put("submissionDate", application.getSubmissionDate());

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Approve volunteer application",
            description = "Approves a volunteer application and creates a volunteer account. Admin access only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application approved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
            {
                "status": "success",
                "message": "Application approved and volunteer account created",
                "applicationId": 1,
                "userId": 1,
                "initialPassword": "streetmed@pitt"
            }
            """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - Admin only"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PostMapping("/approve")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> approveApplication(
            @RequestBody @Schema(example = """
            {
                "adminUsername": "admin",
                "authenticated": "true",
                "applicationId": "1"
            }
            """) Map<String, String> approvalData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = approvalData.get("adminUsername");
                String authStatus = approvalData.get("authenticated");
                String applicationId = approvalData.get("applicationId");

                if (adminUsername == null || authStatus == null || applicationId == null) {
                    throw new RuntimeException("Missing required fields");
                }

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    throw new RuntimeException("Unauthorized access");
                }

                Optional<VolunteerApplication> applicationOpt = volunteerApplicationService
                        .getApplicationById(Integer.parseInt(applicationId));
                if (applicationOpt.isEmpty()) {
                    throw new RuntimeException("Application not found");
                }

                VolunteerApplication application = applicationOpt.get();

                // Create new user account for the volunteer
                User newVolunteer = new User();
                newVolunteer.setUsername(application.getEmail());
                newVolunteer.setEmail(application.getEmail());
                newVolunteer.setPassword(INITIAL_PASSWORD);
                newVolunteer.setPhone(application.getPhone());
                newVolunteer.setRole("VOLUNTEER");

                // Create and properly set up metadata with all required fields
                UserMetadata metadata = new UserMetadata();
                metadata.setFirstName(application.getFirstName());
                metadata.setLastName(application.getLastName());
                metadata.setCreatedAt(LocalDateTime.now());  // Explicitly set creation time
                metadata.setLastLogin(LocalDateTime.now());  // Set initial last login
                metadata.setUser(newVolunteer);  // Set up bidirectional relationship

                newVolunteer.setMetadata(metadata);  // This will handle the bidirectional relationship

                User savedVolunteer = userService.createUser(newVolunteer);

                // Approve the application and link it to the new user
                VolunteerApplication approvedApplication = volunteerApplicationService
                        .approveApplication(Integer.parseInt(applicationId), savedVolunteer);

                // Send approval email with login credentials
                emailService.sendVolunteerApprovalEmail(
                        application.getEmail(),
                        application.getFirstName(),
                        application.getLastName()
                );

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Application approved and volunteer account created");
                response.put("applicationId", approvedApplication.getApplicationId());
                response.put("userId", savedVolunteer.getUserId());
                response.put("initialPassword", INITIAL_PASSWORD);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        }, authExecutor);
    }

    @Operation(summary = "Reject volunteer application",
            description = "Rejects a volunteer application. Admin access only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application rejected successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                {
                    "status": "success",
                    "message": "Application rejected",
                    "applicationId": 1
                }
                """))),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access - Admin only"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PostMapping("/reject")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> rejectApplication(
            @RequestBody @Schema(example = """
                {
                    "adminUsername": "admin",
                    "authenticated": "true",
                    "applicationId": "1"
                }
                """) Map<String, String> rejectData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String adminUsername = rejectData.get("adminUsername");
                String authStatus = rejectData.get("authenticated");
                String applicationId = rejectData.get("applicationId");

                if (adminUsername == null || authStatus == null || applicationId == null) {
                    throw new RuntimeException("Missing required fields");
                }

                if (!"true".equals(authStatus)) {
                    throw new RuntimeException("Not authenticated");
                }

                User admin = userService.findByUsername(adminUsername);
                if (admin == null || !"ADMIN".equals(admin.getRole())) {
                    throw new RuntimeException("Unauthorized access");
                }

                VolunteerApplication rejectedApplication = volunteerApplicationService
                        .rejectApplication(Integer.parseInt(applicationId));

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Application rejected");
                response.put("applicationId", rejectedApplication.getApplicationId());

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