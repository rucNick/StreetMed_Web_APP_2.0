package com.backend.streetmed_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final Executor emailExecutor;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    private boolean emailServiceEnabled = true;

    // Store OTP codes with expiration timestamps
    private final Map<String, OtpData> otpMap = new HashMap<>();

    private static class OtpData {
        private final String otp;
        private final long expirationTime;

        public OtpData(String otp, long expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }

        public String getOtp() {
            return otp;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Qualifier("emailExecutor") Executor emailExecutor) {
        this.mailSender = mailSender;
        this.emailExecutor = emailExecutor;
    }

    // 1. Password Recovery - Generate and send OTP
    public String sendPasswordRecoveryEmail(String email) {
        // Generate a 6-digit OTP
        String otp = generateOtp();

        // Store OTP with 15-minute expiration
        long expirationTime = System.currentTimeMillis() + (15 * 60 * 1000);
        otpMap.put(email, new OtpData(otp, expirationTime));

        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("StreetMed@Pitt Password Recovery");
                    message.setText("Your password recovery code is: " + otp + "\n\n" +
                            "This code will expire in 15 minutes. If you did not request this code, please ignore this email.");

                    mailSender.send(message);
                    logger.info("Password recovery email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send password recovery email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent password recovery OTP: {} to: {}", otp, email);
        }

        return otp;
    }

    // 2. New User Creation - Send credentials
    public void sendNewUserCredentials(String email, String username, String password) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("Welcome to StreetMed - Your Account Details");
                    message.setText("Hello " + username + ",\n\n" +
                            "Your account on StreetMed has been created by an administrator.\n\n" +
                            "Your login credentials are:\n" +
                            "Username: " + username + "\n" +
                            "Password: " + password + "\n\n" +
                            "Please log in and change your password at your earliest convenience for security reasons.\n\n" +
                            "Best regards,\n" +
                            "StreetMed@Pitt Team");

                    mailSender.send(message);
                    logger.info("User credentials email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send user credentials email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent credentials for user: {} to: {}", username, email);
        }
    }

    // 3. Volunteer Application Approval
    public void sendVolunteerApprovalEmail(String email, String firstName, String lastName) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("StreetMed Volunteer Application Approved");
                    message.setText("Dear " + firstName + " " + lastName + ",\n\n" +
                            "We are pleased to inform you that your application to volunteer with StreetMed has been approved!\n\n" +
                            "Your login credentials are:\n" +
                            "Username: " + email + "\n" +
                            "Password: streetmed@pitt\n\n" +
                            "Please log in and change your password at your earliest convenience for security reasons.\n\n" +
                            "Thank you for joining our team. We look forward to working with you!\n\n" +
                            "Best regards,\n" +
                            "StreetMed@Pitt Team");

                    mailSender.send(message);
                    logger.info("Volunteer approval email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send volunteer approval email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent volunteer approval email to: {} ({} {})",
                    email, firstName, lastName);
        }
    }

    // 4. Round Signup Confirmation
    public void sendRoundSignupConfirmationEmail(String email, Map<String, Object> roundData) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    String roundTitle = (String) roundData.get("roundTitle");
                    String startTime = formatDateTime((java.time.LocalDateTime) roundData.get("startTime"));
                    String location = (String) roundData.get("location");
                    String status = (String) roundData.get("status");

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("StreetMed Round Signup Confirmation");

                    StringBuilder messageText = new StringBuilder();
                    messageText.append("Thank you for signing up for the upcoming StreetMed round.\n\n");
                    messageText.append("Round Details:\n");
                    messageText.append("Title: ").append(roundTitle).append("\n");
                    messageText.append("Time: ").append(startTime).append("\n");
                    messageText.append("Location: ").append(location).append("\n");
                    messageText.append("Status: ").append(status).append("\n\n");

                    if ("CONFIRMED".equals(status)) {
                        messageText.append("Your signup has been confirmed. We look forward to seeing you there!\n\n");
                        messageText.append("Please remember that if you need to cancel, you must do so at least 24 hours before the round.");
                    } else if ("WAITLISTED".equals(status)) {
                        messageText.append("You have been added to the waitlist. We will notify you if a spot becomes available.");
                    }

                    messageText.append("\n\nBest regards,\nStreetMed@Pitt Team");

                    message.setText(messageText.toString());
                    mailSender.send(message);
                    logger.info("Round signup confirmation email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send round signup confirmation email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent round signup confirmation email to: {}", email);
        }
    }

    // 5. Round Cancellation Notification
    public void sendRoundCancellationEmail(String email, Map<String, Object> roundData) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    String roundTitle = (String) roundData.get("roundTitle");
                    String startTime = formatDateTime((java.time.LocalDateTime) roundData.get("startTime"));
                    String location = (String) roundData.get("location");

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("StreetMed Round Cancellation Notice");

                    StringBuilder messageText = new StringBuilder();
                    messageText.append("Important Notice: A StreetMed round you signed up for has been cancelled.\n\n");
                    messageText.append("Round Details:\n");
                    messageText.append("Title: ").append(roundTitle).append("\n");
                    messageText.append("Time: ").append(startTime).append("\n");
                    messageText.append("Location: ").append(location).append("\n\n");
                    messageText.append("We apologize for any inconvenience this may cause. ");
                    messageText.append("Please check the website for other upcoming rounds you might be interested in joining.\n\n");
                    messageText.append("Best regards,\nStreetMed@Pitt Team");

                    message.setText(messageText.toString());
                    mailSender.send(message);
                    logger.info("Round cancellation email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send round cancellation email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent round cancellation email to: {}", email);
        }
    }

    // 6. Lottery Selection Notification
    public void sendLotteryWinEmail(String email, Map<String, Object> roundData) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    String roundTitle = (String) roundData.get("roundTitle");
                    String startTime = formatDateTime((java.time.LocalDateTime) roundData.get("startTime"));
                    String location = (String) roundData.get("location");

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("Good News: You're Confirmed for StreetMed Round");

                    StringBuilder messageText = new StringBuilder();
                    messageText.append("Good news! A spot has opened up for a StreetMed round you were waitlisted for, ");
                    messageText.append("and you have been selected to participate.\n\n");
                    messageText.append("Round Details:\n");
                    messageText.append("Title: ").append(roundTitle).append("\n");
                    messageText.append("Time: ").append(startTime).append("\n");
                    messageText.append("Location: ").append(location).append("\n\n");
                    messageText.append("Your status has been updated from WAITLISTED to CONFIRMED.\n\n");
                    messageText.append("Please remember that if you need to cancel, you must do so at least 24 hours before the round.\n\n");
                    messageText.append("We look forward to seeing you there!\n\n");
                    messageText.append("Best regards,\nStreetMed@Pitt Team");

                    message.setText(messageText.toString());
                    mailSender.send(message);
                    logger.info("Lottery win email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send lottery win email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent lottery win email to: {}", email);
        }
    }

    // 7. Reminder Email for Upcoming Round
    public void sendRoundReminderEmail(String email, Map<String, Object> roundData) {
        if (emailServiceEnabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    String roundTitle = (String) roundData.get("roundTitle");
                    String startTime = formatDateTime((java.time.LocalDateTime) roundData.get("startTime"));
                    String location = (String) roundData.get("location");
                    String role = (String) roundData.get("role");

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    message.setSubject("Reminder: Upcoming StreetMed Round");

                    StringBuilder messageText = new StringBuilder();
                    messageText.append("This is a friendly reminder about your upcoming StreetMed round.\n\n");
                    messageText.append("Round Details:\n");
                    messageText.append("Title: ").append(roundTitle).append("\n");
                    messageText.append("Time: ").append(startTime).append(" (Tomorrow)\n");
                    messageText.append("Location: ").append(location).append("\n");

                    if (role != null && !role.equals("VOLUNTEER")) {
                        messageText.append("Your Role: ").append(role).append("\n");
                    }

                    messageText.append("\nPlease remember that if you need to cancel, you must do so at least 24 hours before the round. ");
                    messageText.append("After that time, cancellations are not permitted except for emergencies.\n\n");
                    messageText.append("We look forward to seeing you there!\n\n");
                    messageText.append("Best regards,\nStreetMed@Pitt Team");

                    message.setText(messageText.toString());
                    mailSender.send(message);
                    logger.info("Round reminder email sent to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send round reminder email to {}: {}", email, e.getMessage());
                }
            }, emailExecutor);
        } else {
            logger.info("Email service is disabled. Would have sent round reminder email to: {}", email);
        }
    }

    // Verify OTP
    public boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpMap.get(email);

        if (otpData == null || otpData.isExpired()) {
            return false;
        }

        boolean isValid = otpData.getOtp().equals(otp);

        // Remove OTP after verification (whether successful or not)
        if (isValid || otpData.isExpired()) {
            otpMap.remove(email);
        }

        return isValid;
    }

    // Helper method to generate a 6-digit OTP
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a 6-digit number
        return String.valueOf(otp);
    }

    // Helper method to format date and time
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Not specified";
        }
        return dateTime.format(dateTimeFormatter);
    }

    // Check if email service is enabled
    public boolean isEmailServiceEnabled() {
        return emailServiceEnabled;
    }

    // Set email service status
    public void setEmailServiceEnabled(boolean enabled) {
        this.emailServiceEnabled = enabled;
        logger.info("Email service status set to: {}", enabled ? "enabled" : "disabled");
    }
}