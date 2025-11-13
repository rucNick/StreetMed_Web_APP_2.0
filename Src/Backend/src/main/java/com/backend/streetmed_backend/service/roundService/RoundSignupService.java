package com.backend.streetmed_backend.service.roundService;

import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import com.backend.streetmed_backend.entity.rounds_entity.RoundSignup;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundsRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundSignupRepository;
import com.backend.streetmed_backend.repository.User.UserRepository;
import com.backend.streetmed_backend.repository.User.VolunteerSubRoleRepository;
import com.backend.streetmed_backend.service.EmailService;
import com.backend.streetmed_backend.service.orderService.OrderRoundAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class RoundSignupService {
    private final RoundsRepository roundsRepository;
    private final RoundSignupRepository roundSignupRepository;
    private final UserRepository userRepository;
    private final VolunteerSubRoleRepository volunteerSubRoleRepository;
    private final EmailService emailService;
    private final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(RoundSignupService.class);

    @Autowired
    private OrderRoundAssignmentService orderRoundAssignmentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    public RoundSignupService(RoundsRepository roundsRepository,
                              RoundSignupRepository roundSignupRepository,
                              UserRepository userRepository,
                              VolunteerSubRoleRepository volunteerSubRoleRepository,
                              EmailService emailService) {
        this.roundsRepository = roundsRepository;
        this.roundSignupRepository = roundSignupRepository;
        this.userRepository = userRepository;
        this.volunteerSubRoleRepository = volunteerSubRoleRepository;
        this.emailService = emailService;
    }

    /**
     * Get a signup by ID
     */
    public RoundSignup findSignupById(Integer signupId) {
        return roundSignupRepository.findById(signupId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));
    }

    /**
     * Update a signup
     */
    public RoundSignup updateSignup(RoundSignup signup) {
        return roundSignupRepository.save(signup);
    }

    /**
     * Get all signups for a round
     */
    public List<RoundSignup> getAllSignupsForRound(Integer roundId) {
        return roundSignupRepository.findByRoundId(roundId);
    }

    /**
     * Get team lead for a round
     */
    public Optional<RoundSignup> getTeamLeadForRound(Integer roundId) {
        return roundSignupRepository.findTeamLeadForRound(roundId);
    }

    /**
     * Get clinician for a round
     */
    public Optional<RoundSignup> getClinicianForRound(Integer roundId) {
        return roundSignupRepository.findClinicianForRound(roundId);
    }

    /**
     * Check if a round has a team lead
     */
    public boolean hasTeamLead(Integer roundId) {
        return roundSignupRepository.hasTeamLead(roundId);
    }

    /**
     * Check if a round has a clinician
     */
    public boolean hasClinician(Integer roundId) {
        return roundSignupRepository.hasClinician(roundId);
    }

    /**
     * Add user details to a signup map
     */
    public void addUserDetailsToSignup(Map<String, Object> signupDetails, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        signupDetails.put("username", user.getUsername());
        signupDetails.put("email", user.getEmail());
        signupDetails.put("phone", user.getPhone());

        UserMetadata metadata = user.getMetadata();
        if (metadata != null) {
            signupDetails.put("firstName", metadata.getFirstName());
            signupDetails.put("lastName", metadata.getLastName());
        }
    }

    /**
     * Admin cancel signup - bypasses 24 hour restriction
     */
    @Transactional
    public void adminCancelSignup(Integer signupId, Integer adminId) {
        RoundSignup signup = roundSignupRepository.findById(signupId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));

        Rounds round = roundsRepository.findById(signup.getRoundId())
                .orElseThrow(() -> new RuntimeException("Round not found"));

        // Delete the signup
        roundSignupRepository.delete(signup);

        // If this was a confirmed regular volunteer, run lottery to fill the spot
        if ("CONFIRMED".equals(signup.getStatus()) && "VOLUNTEER".equals(signup.getRole())) {
            runLotteryForRound(signup.getRoundId());
        }

        // Notify the user - send email if enabled
        try {
            User user = userRepository.findById(signup.getUserId()).orElse(null);
            if (user != null && user.getEmail() != null && emailService.isEmailServiceEnabled()) {
                Map<String, Object> emailData = new HashMap<>();
                emailData.put("roundTitle", round.getTitle());
                emailData.put("startTime", round.getStartTime());
                emailData.put("location", round.getLocation());
                emailData.put("action", "removed by administrator");

                // Send email notification in a non-blocking way
                CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendRoundCancellationEmail(user.getEmail(), emailData);
                    } catch (Exception e) {
                        logger.error("Failed to send removal notification email to {}: {}",
                                user.getEmail(), e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error notifying user {} about removal: {}", signup.getUserId(), e.getMessage());
        }
    }

    /**
     * Check if a user has already signed up for a round
     */
    public boolean isUserSignedUp(Integer roundId, Integer userId) {
        return roundSignupRepository.existsByRoundIdAndUserId(roundId, userId);
    }

    /**
     * Count confirmed volunteers for a round (excluding team lead and clinician)
     */
    public long countConfirmedVolunteersForRound(Integer roundId) {
        return roundSignupRepository.countConfirmedVolunteersForRound(roundId);
    }

    /**
     * Get signup details for a user in a round
     */
    public Map<String, Object> getUserSignupDetails(Integer roundId, Integer userId) {
        RoundSignup signup = roundSignupRepository.findByRoundIdAndUserId(roundId, userId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("signupId", signup.getSignupId());
        details.put("role", signup.getRole());
        details.put("status", signup.getStatus());
        details.put("signupTime", signup.getSignupTime());

        if ("WAITLISTED".equals(signup.getStatus()) && signup.getLotteryNumber() != null) {
            details.put("lotteryNumber", signup.getLotteryNumber());

            // Get position in waitlist
            List<RoundSignup> waitlist = roundSignupRepository.findByRoundIdAndStatusOrderByLotteryNumberAsc(
                    roundId, "WAITLISTED");

            int position = 0;
            for (int i = 0; i < waitlist.size(); i++) {
                if (waitlist.get(i).getSignupId().equals(signup.getSignupId())) {
                    position = i + 1; // 1-based position
                    break;
                }
            }

            details.put("waitlistPosition", position);
        }

        return details;
    }

    /**
     * Get rounds that a volunteer has signed up for
     */
    public List<Map<String, Object>> getVolunteerSignups(Integer userId) {
        List<RoundSignup> signups = roundSignupRepository.findByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (RoundSignup signup : signups) {
            try {
                Rounds round = roundsRepository.findById(signup.getRoundId()).orElse(null);
                if (round != null) {
                    // Skip cancelled rounds
                    if ("CANCELED".equals(round.getStatus()) || "CANCELLED".equals(round.getStatus())) {
                        continue;
                    }

                    Map<String, Object> roundInfo = new HashMap<>();

                    // Basic round info
                    roundInfo.put("roundId", round.getRoundId());
                    roundInfo.put("title", round.getTitle());
                    roundInfo.put("description", round.getDescription());
                    roundInfo.put("startTime", round.getStartTime());
                    roundInfo.put("endTime", round.getEndTime());
                    roundInfo.put("location", round.getLocation());
                    roundInfo.put("status", round.getStatus());

                    // Add actual order count
                    long actualOrderCount = orderRepository.countByRoundId(round.getRoundId());
                    roundInfo.put("currentOrderCount", actualOrderCount);
                    roundInfo.put("orderCapacity", round.getOrderCapacity() != null ? round.getOrderCapacity() : 20);

                    // Signup info
                    roundInfo.put("signupId", signup.getSignupId());
                    roundInfo.put("role", signup.getRole());
                    roundInfo.put("signupStatus", signup.getStatus());
                    roundInfo.put("signupTime", signup.getSignupTime());

                    // Check if the round is in the past
                    boolean isPast = round.getEndTime().isBefore(LocalDateTime.now());
                    roundInfo.put("isPast", isPast);

                    // Check if signup can be canceled (24+ hours in advance)
                    boolean canCancel = false;
                    if (!isPast && "CONFIRMED".equals(signup.getStatus())) {
                        LocalDateTime now = LocalDateTime.now();
                        long hoursUntilRound = ChronoUnit.HOURS.between(now, round.getStartTime());
                        canCancel = hoursUntilRound >= 24;
                    }
                    roundInfo.put("canCancel", canCancel);

                    // Special role info
                    boolean isTeamLead = "TEAM_LEAD".equals(signup.getRole());
                    boolean isClinician = "CLINICIAN".equals(signup.getRole());
                    roundInfo.put("isTeamLead", isTeamLead);
                    roundInfo.put("isClinician", isClinician);

                    result.add(roundInfo);
                }
            } catch (Exception e) {
                logger.error("Error fetching round details for signup {}: {}",
                        signup.getSignupId(), e.getMessage());
            }
        }

        // Sort by start time (newest first)
        result.sort((map1, map2) -> {
            LocalDateTime dt1 = (LocalDateTime) map1.get("startTime");
            LocalDateTime dt2 = (LocalDateTime) map2.get("startTime");
            return dt2.compareTo(dt1);
        });

        return result;
    }

    /**
     * Volunteer signup for a round
     */
    @Transactional
    public RoundSignup signupForRound(Integer roundId, Integer userId, String requestedRole) {
        Rounds round = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is a volunteer
        if (!"VOLUNTEER".equals(user.getRole())) {
            throw new RuntimeException("Only volunteers can sign up for rounds");
        }

        // Check if the round is in the future
        if (round.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot sign up for past rounds");
        }

        // Check if the round is scheduled (not canceled)
        if (!"SCHEDULED".equals(round.getStatus())) {
            throw new RuntimeException("Cannot sign up for " + round.getStatus().toLowerCase() + " rounds");
        }

        // Check if user already signed up for this round
        if (roundSignupRepository.existsByRoundIdAndUserId(roundId, userId)) {
            throw new RuntimeException("You have already signed up for this round");
        }

        // Determine role and validate it
        String role = determineUserRole(userId, requestedRole);

        RoundSignup signup = new RoundSignup(roundId, userId, role);
        signup.setSignupTime(LocalDateTime.now());

        // Special handling for TEAM_LEAD and CLINICIAN roles
        if ("TEAM_LEAD".equals(role)) {
            handleTeamLeadSignup(round, userId, signup);
        } else if ("CLINICIAN".equals(role)) {
            handleClinicianSignup(round, userId, signup);
        } else {
            // Regular volunteer signup - always waitlist first
            handleRegularVolunteerSignup(round, signup);
        }

        RoundSignup savedSignup = roundSignupRepository.save(signup);

        // Send confirmation email
        if (emailService.isEmailServiceEnabled() && user.getEmail() != null) {
            String status = savedSignup.getStatus();
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("roundTitle", round.getTitle());
            emailData.put("startTime", round.getStartTime());
            emailData.put("location", round.getLocation());
            emailData.put("status", status);

            // Send email notification in a non-blocking way
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendRoundSignupConfirmationEmail(user.getEmail(), emailData);
                } catch (Exception e) {
                    logger.error("Failed to send signup confirmation email to {}: {}", user.getEmail(), e.getMessage());
                }
            });
        }

        return savedSignup;
    }

    /**
     * Determine the appropriate role for a user
     */
    private String determineUserRole(Integer userId, String requestedRole) {
        if (requestedRole == null || requestedRole.isEmpty()) {
            return "VOLUNTEER";
        }

        switch (requestedRole.toUpperCase()) {
            case "TEAM_LEAD":
                if (!volunteerSubRoleRepository.existsByUserIdAndSubRole(userId, VolunteerSubRole.SubRoleType.TEAM_LEAD)) {
                    throw new RuntimeException("User does not have TEAM_LEAD privileges");
                }
                return "TEAM_LEAD";
            case "CLINICIAN":
                if (!volunteerSubRoleRepository.existsByUserIdAndSubRole(userId, VolunteerSubRole.SubRoleType.CLINICIAN)) {
                    throw new RuntimeException("User does not have CLINICIAN privileges");
                }
                return "CLINICIAN";
            default:
                return "VOLUNTEER";
        }
    }

    /**
     * Handle signup for Team Lead role
     */
    private void handleTeamLeadSignup(Rounds round, Integer userId, RoundSignup signup) {
        // If already has a team lead, reject
        if (hasTeamLead(round.getRoundId())) {
            throw new RuntimeException("This round already has a team lead assigned");
        }

        // Confirm signup immediately
        signup.setStatus("CONFIRMED");
    }

    /**
     * Handle signup for Clinician role
     */
    private void handleClinicianSignup(Rounds round, Integer userId, RoundSignup signup) {
        // If already has a clinician, reject
        if (hasClinician(round.getRoundId())) {
            throw new RuntimeException("This round already has a clinician assigned");
        }

        // Confirm signup immediately
        signup.setStatus("CONFIRMED");
    }

    /**
     * Handle signup for regular volunteer
     */
    private void handleRegularVolunteerSignup(Rounds round, RoundSignup signup) {
        signup.setStatus("WAITLISTED");
        signup.setLotteryNumber(generateLotteryNumber());
    }

    /**
     * Generate a random lottery number for the waitlist
     */
    private Integer generateLotteryNumber() {
        return random.nextInt(10000);
    }

    /**
     * Run the lottery to fill available slots
     */
    @Transactional
    public List<RoundSignup> runLotteryForRound(Integer roundId) {
        Rounds round = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found"));

        // Calculate available slots
        long confirmedParticipants = roundSignupRepository.countConfirmedVolunteersForRound(roundId);
        int availableSlots = round.getMaxParticipants() - (int)confirmedParticipants;

        if (availableSlots <= 0) {
            return Collections.emptyList();
        }

        // Get waitlisted signups ordered by lottery number
        List<RoundSignup> waitlistedSignups = roundSignupRepository.findByRoundIdAndStatusOrderByLotteryNumberAsc(roundId, "WAITLISTED");

        List<RoundSignup> selectedSignups = new ArrayList<>();

        // Move volunteers from waitlist to confirmed
        for (int i = 0; i < Math.min(availableSlots, waitlistedSignups.size()); i++) {
            RoundSignup signup = waitlistedSignups.get(i);
            signup.setStatus("CONFIRMED");
            signup.setUpdatedAt(LocalDateTime.now());
            roundSignupRepository.save(signup);
            selectedSignups.add(signup);

            // Send confirmation email
            try {
                User user = userRepository.findById(signup.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null && emailService.isEmailServiceEnabled()) {
                    Map<String, Object> emailData = new HashMap<>();
                    emailData.put("roundTitle", round.getTitle());
                    emailData.put("startTime", round.getStartTime());
                    emailData.put("location", round.getLocation());

                    // Send email notification in a non-blocking way
                    CompletableFuture.runAsync(() -> {
                        try {
                            emailService.sendLotteryWinEmail(user.getEmail(), emailData);
                        } catch (Exception e) {
                            logger.error("Failed to send lottery win email to {}: {}", user.getEmail(), e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error notifying user {} about lottery selection: {}", signup.getUserId(), e.getMessage());
            }
        }

        return selectedSignups;
    }

    /**
     * Cancel a signup (volunteer wants to quit)
     */
    @Transactional
    public void cancelSignup(Integer signupId, Integer userId) {
        RoundSignup signup = roundSignupRepository.findById(signupId)
                .orElseThrow(() -> new RuntimeException("Signup not found"));

        // Verify the signup belongs to the user
        if (!signup.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this signup");
        }

        Rounds round = roundsRepository.findById(signup.getRoundId())
                .orElseThrow(() -> new RuntimeException("Round not found"));

        // Check if cancellation is allowed (must be at least 24 hours before round)
        LocalDateTime now = LocalDateTime.now();
        if (ChronoUnit.HOURS.between(now, round.getStartTime()) < 24) {
            throw new RuntimeException("Cannot cancel signup less than 24 hours before the round");
        }

        Integer roundId = signup.getRoundId(); // Store the roundId before deleting the signup

        // Delete the signup
        roundSignupRepository.delete(signup);

        // If this was a confirmed regular volunteer, run lottery to fill the spot
        if ("CONFIRMED".equals(signup.getStatus()) && "VOLUNTEER".equals(signup.getRole())) {
            runLotteryForRound(signup.getRoundId());
        }

        // After successful cancellation, trigger order rebalancing
        orderRoundAssignmentService.handleVolunteerCancellation(roundId);
    }

    /**
     * Get confirmed signups for a round
     */
    public List<RoundSignup> getConfirmedSignups(Integer roundId) {
        return roundSignupRepository.findByRoundIdAndStatusOrderBySignupTimeAsc(roundId, "CONFIRMED");
    }

    /**
     * Get waitlisted signups for a round
     */
    public List<RoundSignup> getWaitlistedSignups(Integer roundId) {
        return roundSignupRepository.findByRoundIdAndStatusOrderByLotteryNumberAsc(roundId, "WAITLISTED");
    }

    /**
     * Check if a volunteer has the team lead role
     */
    public boolean hasTeamLeadRole(Integer userId) {
        return volunteerSubRoleRepository.existsByUserIdAndSubRole(userId, VolunteerSubRole.SubRoleType.TEAM_LEAD);
    }

    /**
     * Check if a volunteer has the clinician role
     */
    public boolean hasClinicianRole(Integer userId) {
        return volunteerSubRoleRepository.existsByUserIdAndSubRole(userId, VolunteerSubRole.SubRoleType.CLINICIAN);
    }

    /**
     * Find a user's signup for a specific round
     * @param roundId The round ID
     * @param userId The user ID
     * @return Optional containing the signup if found
     */
    public Optional<RoundSignup> findByRoundIdAndUserId(Integer roundId, Integer userId) {
        return roundSignupRepository.findByRoundIdAndUserId(roundId, userId);
    }
}