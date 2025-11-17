package com.backend.streetmed_backend.repository.Rounds;

import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoundsRepository extends JpaRepository<Rounds, Integer> {

    // Find upcoming rounds (start time is in the future)
    List<Rounds> findByStartTimeAfterAndStatusOrderByStartTimeAsc(LocalDateTime now, String status);

    // Find rounds by status
    List<Rounds> findByStatus(String status);

    // Count upcoming rounds
    @Query("SELECT COUNT(r) FROM Rounds r WHERE r.startTime > ?1 AND r.status = 'SCHEDULED'")
    long countUpcomingRounds(LocalDateTime now);

    // Find rounds for a specific date range (useful for calendar views)
    @Query("SELECT r FROM Rounds r WHERE r.startTime BETWEEN :startDate AND :endDate ORDER BY r.startTime ASC")
    List<Rounds> findRoundsForDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find rounds for the next 7 days
    @Query("SELECT r FROM Rounds r WHERE r.startTime BETWEEN ?1 AND ?2 AND r.status = 'SCHEDULED' ORDER BY r.startTime ASC")
    List<Rounds> findRoundsForNext7Days(LocalDateTime start, LocalDateTime end);

    // Find rounds with availability (has slots open)
    @Query("SELECT r FROM Rounds r WHERE r.startTime > :now AND r.status = 'SCHEDULED' " +
            "AND (SELECT COUNT(rs) FROM RoundSignup rs WHERE rs.roundId = r.roundId AND rs.status = 'CONFIRMED' AND rs.role = 'VOLUNTEER') < r.maxParticipants " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findAvailableRounds(@Param("now") LocalDateTime now);

    // Find rounds where a specific user is participating (any role)
    @Query("SELECT r FROM Rounds r JOIN RoundSignup rs ON r.roundId = rs.roundId " +
            "WHERE rs.userId = :userId AND rs.status = 'CONFIRMED' " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findRoundsForUser(@Param("userId") Integer userId);

    // Find rounds where a specific user is a team lead
    @Query("SELECT r FROM Rounds r JOIN RoundSignup rs ON r.roundId = rs.roundId " +
            "WHERE rs.userId = :userId AND rs.role = 'TEAM_LEAD' AND rs.status = 'CONFIRMED' " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findRoundsWhereUserIsTeamLead(@Param("userId") Integer userId);

    // Find rounds where a specific user is a clinician
    @Query("SELECT r FROM Rounds r JOIN RoundSignup rs ON r.roundId = rs.roundId " +
            "WHERE rs.userId = :userId AND rs.role = 'CLINICIAN' AND rs.status = 'CONFIRMED' " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findRoundsWhereUserIsClinician(@Param("userId") Integer userId);

    // Find rounds that need a team lead
    @Query("SELECT r FROM Rounds r WHERE r.startTime > :now AND r.status = 'SCHEDULED' " +
            "AND NOT EXISTS (SELECT rs FROM RoundSignup rs WHERE rs.roundId = r.roundId AND rs.role = 'TEAM_LEAD' AND rs.status = 'CONFIRMED') " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findRoundsNeedingTeamLead(@Param("now") LocalDateTime now);

    // Find rounds that need a clinician
    @Query("SELECT r FROM Rounds r WHERE r.startTime > :now AND r.status = 'SCHEDULED' " +
            "AND NOT EXISTS (SELECT rs FROM RoundSignup rs WHERE rs.roundId = r.roundId AND rs.role = 'CLINICIAN' AND rs.status = 'CONFIRMED') " +
            "ORDER BY r.startTime ASC")
    List<Rounds> findRoundsNeedingClinician(@Param("now") LocalDateTime now);
}