package com.backend.streetmed_backend.repository.Rounds;

import com.backend.streetmed_backend.entity.rounds_entity.RoundCapacityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoundCapacityConfigRepository extends JpaRepository<RoundCapacityConfig, Integer> {

    Optional<RoundCapacityConfig> findByRoundId(Integer roundId);

    @Query("SELECT COALESCE(r.overrideCapacity, r.maxOrdersPerVolunteer * :volunteerCount, 0) " +
            "FROM RoundCapacityConfig r WHERE r.roundId = :roundId")
    Integer calculateTotalCapacity(Integer roundId, Long volunteerCount);
}