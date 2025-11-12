package com.backend.streetmed_backend.repository.Rounds;

import com.backend.streetmed_backend.entity.rounds_entity.RoundSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundSignupRepository extends JpaRepository<RoundSignup, Integer> {

    // Find all signups for a specific round
    List<RoundSignup> findByRoundId(Integer roundId);

    // Find all signups for a specific user
    List<RoundSignup> findByUserId(Integer userId);

    // Find a user's signup for a specific round
    Optional<RoundSignup> findByRoundIdAndUserId(Integer roundId, Integer userId);
    // Find confirmed signups for a round
    List<RoundSignup> findByRoundIdAndStatusOrderBySignupTimeAsc(Integer roundId, String status);

    // Count confirmed participants for a round (not including team lead or clinician)
    @Query("SELECT COUNT(rs) FROM RoundSignup rs WHERE rs.roundId = :roundId AND rs.status = 'CONFIRMED' AND rs.role = 'VOLUNTEER'")
    long countConfirmedVolunteersForRound(@Param("roundId") Integer roundId);

    // Find team lead for a round (if exists)
    @Query("SELECT rs FROM RoundSignup rs WHERE rs.roundId = :roundId AND rs.role = 'TEAM_LEAD' AND rs.status = 'CONFIRMED'")
    Optional<RoundSignup> findTeamLeadForRound(@Param("roundId") Integer roundId);

    // Find clinician for a round (if exists)
    @Query("SELECT rs FROM RoundSignup rs WHERE rs.roundId = :roundId AND rs.role = 'CLINICIAN' AND rs.status = 'CONFIRMED'")
    Optional<RoundSignup> findClinicianForRound(@Param("roundId") Integer roundId);

    // Find all signups for a round with specific status ordered by lottery number
    List<RoundSignup> findByRoundIdAndStatusOrderByLotteryNumberAsc(Integer roundId, String status);

    // Check if a user already signed up for a round with any role
    boolean existsByRoundIdAndUserId(Integer roundId, Integer userId);

    // Check if a round has a team lead
    @Query("SELECT COUNT(rs) > 0 FROM RoundSignup rs WHERE rs.roundId = :roundId AND rs.role = 'TEAM_LEAD' AND rs.status = 'CONFIRMED'")
    boolean hasTeamLead(@Param("roundId") Integer roundId);

    // Check if a round has a clinician
    @Query("SELECT COUNT(rs) > 0 FROM RoundSignup rs WHERE rs.roundId = :roundId AND rs.role = 'CLINICIAN' AND rs.status = 'CONFIRMED'")
    boolean hasClinician(@Param("roundId") Integer roundId);
}