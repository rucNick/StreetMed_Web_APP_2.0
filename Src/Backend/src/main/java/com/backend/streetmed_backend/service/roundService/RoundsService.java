package com.backend.streetmed_backend.service.roundService;

import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import com.backend.streetmed_backend.entity.rounds_entity.RoundSignup;
import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.repository.Rounds.RoundsRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundSignupRepository;
import com.backend.streetmed_backend.repository.User.UserRepository;
import com.backend.streetmed_backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class RoundsService {
    private final RoundsRepository roundsRepository;
    private final RoundSignupRepository roundSignupRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RoundSignupService roundSignupService;
    private static final Logger logger = LoggerFactory.getLogger(RoundsService.class);

    @Autowired
    public RoundsService(RoundsRepository roundsRepository,
                         RoundSignupRepository roundSignupRepository,
                         UserRepository userRepository,
                         EmailService emailService,
                         RoundSignupService roundSignupService) {
        this.roundsRepository = roundsRepository;
        this.roundSignupRepository = roundSignupRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.roundSignupService = roundSignupService;
    }

    /**
     * Get all rounds (including past, upcoming, and canceled)
     */
    public List<Rounds> getAllRounds() {
        return roundsRepository.findAll();
    }

    /**
     * Get rounds for a specific date range (for calendar)
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return A list of rounds within the date range
     */
    public List<Rounds> getRoundsForDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return roundsRepository.findRoundsForDateRange(startDate, endDate);
    }

    /**
     * Get rounds for a specific month (for monthly calendar view)
     * @param year The year
     * @param month The month (1-12)
     * @return A list of rounds for the month
     */
    public List<Rounds> getRoundsForMonth(int year, int month) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth;
        if (month == 12) {
            endOfMonth = LocalDateTime.of(year + 1, 1, 1, 0, 0).minusNanos(1);
        } else {
            endOfMonth = LocalDateTime.of(year, month + 1, 1, 0, 0).minusNanos(1);
        }

        return getRoundsForDateRange(startOfMonth, endOfMonth);
    }

    /**
     * Get rounds for a specific day (for daily view)
     * @param date The date
     * @return A list of rounds for the day
     */
    public List<Rounds> getRoundsForDay(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return getRoundsForDateRange(startOfDay, endOfDay);
    }

    /**
     * Create a new rounds schedule
     */
    public Rounds createRound(Rounds round) {
        // Validate required fields
        if (round.getStartTime() == null || round.getEndTime() == null ||
                round.getLocation() == null || round.getTitle() == null ||
                round.getMaxParticipants() == null) {
            throw new IllegalArgumentException("Missing required fields for creating a round");
        }

        // Set default values
        round.setCreatedAt(LocalDateTime.now());
        round.setUpdatedAt(LocalDateTime.now());
        round.setStatus("SCHEDULED");

        return roundsRepository.save(round);
    }

    /**
     * Update an existing rounds schedule
     */
    public Rounds updateRound(Integer roundId, Rounds updatedRound) {
        Rounds existingRound = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found with ID: " + roundId));

        // Update fields
        existingRound.setTitle(updatedRound.getTitle());
        existingRound.setDescription(updatedRound.getDescription());
        existingRound.setStartTime(updatedRound.getStartTime());
        existingRound.setEndTime(updatedRound.getEndTime());
        existingRound.setLocation(updatedRound.getLocation());
        existingRound.setMaxParticipants(updatedRound.getMaxParticipants());
        existingRound.setUpdatedAt(LocalDateTime.now());

        // Set status if provided
        if (updatedRound.getStatus() != null) {
            existingRound.setStatus(updatedRound.getStatus());
        }

        return roundsRepository.save(existingRound);
    }

    /**
     * Get a round by ID
     */
    public Rounds getRound(Integer roundId) {
        return roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found with ID: " + roundId));
    }

    /**
     * Get all scheduled rounds
     */
    public List<Rounds> getAllScheduledRounds() {
        return roundsRepository.findByStatus("SCHEDULED");
    }

    /**
     * Get upcoming rounds (those with start time in the future)
     */
    public List<Rounds> getUpcomingRounds() {
        return roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                LocalDateTime.now(), "SCHEDULED");
    }

    /**
     * Cancel a round
     */
    @Transactional
    public Rounds cancelRound(Integer roundId) {
        Rounds round = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found with ID: " + roundId));

        round.setStatus("CANCELED");
        round.setUpdatedAt(LocalDateTime.now());

        // Update all signups to canceled
        List<RoundSignup> signups = roundSignupRepository.findByRoundId(roundId);
        for (RoundSignup signup : signups) {
            signup.setStatus("CANCELED");
            signup.setUpdatedAt(LocalDateTime.now());
            roundSignupRepository.save(signup);

            // Notify user about cancellation
            try {
                User user = userRepository.findById(signup.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null && emailService.isEmailServiceEnabled()) {
                    // Create a simple email notification
                    Map<String, Object> emailData = new HashMap<>();
                    emailData.put("roundTitle", round.getTitle());
                    emailData.put("startTime", round.getStartTime());
                    emailData.put("location", round.getLocation());
                    // Send email notification in a non-blocking way
                    CompletableFuture.runAsync(() -> {
                        try {
                            emailService.sendRoundCancellationEmail(user.getEmail(), emailData);
                        } catch (Exception e) {
                            logger.error("Failed to send round cancellation email to {}: {}", user.getEmail(), e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error notifying user {} about round cancellation: {}", signup.getUserId(), e.getMessage());
            }
        }

        return roundsRepository.save(round);
    }

    /**
     * Complete a round (mark as completed)
     */
    public Rounds completeRound(Integer roundId) {
        Rounds round = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found with ID: " + roundId));

        round.setStatus("COMPLETED");
        round.setUpdatedAt(LocalDateTime.now());
        return roundsRepository.save(round);
    }

    /**
     * Get rounds that need a team lead
     */
    public List<Rounds> getRoundsNeedingTeamLead() {
        return roundsRepository.findRoundsNeedingTeamLead(LocalDateTime.now());
    }

    /**
     * Get rounds that need a clinician
     */
    public List<Rounds> getRoundsNeedingClinician() {
        return roundsRepository.findRoundsNeedingClinician(LocalDateTime.now());
    }

    /**
     * Get rounds where a user is team lead
     */
    public List<Rounds> getRoundsForTeamLead(Integer userId) {
        return roundsRepository.findRoundsWhereUserIsTeamLead(userId);
    }

    /**
     * Get rounds where a user is clinician
     */
    public List<Rounds> getRoundsForClinician(Integer userId) {
        return roundsRepository.findRoundsWhereUserIsClinician(userId);
    }

    /**
     * Get rounds where a user is participating (any role)
     */
    public List<Rounds> getRoundsForUser(Integer userId) {
        return roundsRepository.findRoundsForUser(userId);
    }

    /**
     * Get detailed round information including team lead and clinician details
     */
    public Map<String, Object> getRoundWithDetails(Integer roundId) {
        Rounds round = roundsRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found with ID: " + roundId));

        Map<String, Object> roundDetails = new HashMap<>();
        roundDetails.put("roundId", round.getRoundId());
        roundDetails.put("title", round.getTitle());
        roundDetails.put("description", round.getDescription());
        roundDetails.put("startTime", round.getStartTime());
        roundDetails.put("endTime", round.getEndTime());
        roundDetails.put("location", round.getLocation());
        roundDetails.put("status", round.getStatus());
        roundDetails.put("maxParticipants", round.getMaxParticipants());

        // Calculate availability
        long confirmedVolunteers = roundSignupRepository.countConfirmedVolunteersForRound(roundId);
        int availableSlots = round.getMaxParticipants() - (int)confirmedVolunteers;

        roundDetails.put("confirmedVolunteers", confirmedVolunteers);
        roundDetails.put("availableSlots", availableSlots);
        roundDetails.put("openForSignup", availableSlots > 0);

        // Get team lead information
        Optional<RoundSignup> teamLeadSignup = roundSignupService.getTeamLeadForRound(roundId);
        if (teamLeadSignup.isPresent()) {
            Map<String, Object> teamLeadInfo = new HashMap<>();
            Integer teamLeadId = teamLeadSignup.get().getUserId();
            roundDetails.put("hasTeamLead", true);

            try {
                User teamLead = userRepository.findById(teamLeadId).orElse(null);
                if (teamLead != null) {
                    teamLeadInfo.put("userId", teamLead.getUserId());
                    teamLeadInfo.put("username", teamLead.getUsername());
                    teamLeadInfo.put("email", teamLead.getEmail());
                    teamLeadInfo.put("phone", teamLead.getPhone());

                    if (teamLead.getMetadata() != null) {
                        teamLeadInfo.put("firstName", teamLead.getMetadata().getFirstName());
                        teamLeadInfo.put("lastName", teamLead.getMetadata().getLastName());
                    }

                    roundDetails.put("teamLead", teamLeadInfo);
                }
            } catch (Exception e) {
                logger.error("Error getting team lead details: {}", e.getMessage());
                roundDetails.put("teamLeadId", teamLeadId);
            }
        } else {
            roundDetails.put("hasTeamLead", false);
        }

        // Get clinician information
        Optional<RoundSignup> clinicianSignup = roundSignupService.getClinicianForRound(roundId);
        if (clinicianSignup.isPresent()) {
            Map<String, Object> clinicianInfo = new HashMap<>();
            Integer clinicianId = clinicianSignup.get().getUserId();
            roundDetails.put("hasClinician", true);

            try {
                User clinician = userRepository.findById(clinicianId).orElse(null);
                if (clinician != null) {
                    clinicianInfo.put("userId", clinician.getUserId());
                    clinicianInfo.put("username", clinician.getUsername());
                    clinicianInfo.put("email", clinician.getEmail());
                    clinicianInfo.put("phone", clinician.getPhone());

                    if (clinician.getMetadata() != null) {
                        clinicianInfo.put("firstName", clinician.getMetadata().getFirstName());
                        clinicianInfo.put("lastName", clinician.getMetadata().getLastName());
                    }

                    roundDetails.put("clinician", clinicianInfo);
                }
            } catch (Exception e) {
                logger.error("Error getting clinician details: {}", e.getMessage());
                roundDetails.put("clinicianId", clinicianId);
            }
        } else {
            roundDetails.put("hasClinician", false);
        }

        return roundDetails;
    }

    /**
     * Get all signups with user details for a round
     */
    public List<Map<String, Object>> getRoundSignupsWithUserDetails(Integer roundId) {
        List<RoundSignup> signups = roundSignupRepository.findByRoundId(roundId);
        List<Map<String, Object>> signupsWithDetails = new java.util.ArrayList<>();

        for (RoundSignup signup : signups) {
            Map<String, Object> signupDetails = new HashMap<>();
            signupDetails.put("signupId", signup.getSignupId());
            signupDetails.put("userId", signup.getUserId());
            signupDetails.put("status", signup.getStatus());
            signupDetails.put("role", signup.getRole());
            signupDetails.put("signupTime", signup.getSignupTime());

            if ("WAITLISTED".equals(signup.getStatus()) && signup.getLotteryNumber() != null) {
                signupDetails.put("lotteryNumber", signup.getLotteryNumber());
            }

            // Add user details
            try {
                roundSignupService.addUserDetailsToSignup(signupDetails, signup.getUserId());
            } catch (Exception e) {
                logger.error("Error adding user details for signup {}: {}", signup.getSignupId(), e.getMessage());
            }

            signupsWithDetails.add(signupDetails);
        }

        return signupsWithDetails;
    }
}