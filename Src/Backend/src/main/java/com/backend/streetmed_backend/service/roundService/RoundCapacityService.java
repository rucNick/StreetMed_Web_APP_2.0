package com.backend.streetmed_backend.service.roundService;

import com.backend.streetmed_backend.entity.rounds_entity.RoundCapacityConfig;
import com.backend.streetmed_backend.repository.Rounds.RoundCapacityConfigRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundSignupRepository;
import com.backend.streetmed_backend.repository.Order.OrderAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoundCapacityService {

    private final RoundCapacityConfigRepository capacityConfigRepository;
    private final RoundSignupRepository roundSignupRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;

    @Autowired
    public RoundCapacityService(RoundCapacityConfigRepository capacityConfigRepository,
                                RoundSignupRepository roundSignupRepository,
                                OrderAssignmentRepository orderAssignmentRepository) {
        this.capacityConfigRepository = capacityConfigRepository;
        this.roundSignupRepository = roundSignupRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
    }

    public RoundCapacityConfig getOrCreateConfig(Integer roundId) {
        return capacityConfigRepository.findByRoundId(roundId)
                .orElseGet(() -> {
                    RoundCapacityConfig config = new RoundCapacityConfig(roundId);
                    return capacityConfigRepository.save(config);
                });
    }

    @Transactional
    public RoundCapacityConfig updateCapacity(Integer roundId, Integer maxOrdersPerVolunteer,
                                              Integer adminId) {
        RoundCapacityConfig config = getOrCreateConfig(roundId);
        config.setMaxOrdersPerVolunteer(maxOrdersPerVolunteer);
        config.setLastModifiedBy(adminId);
        return capacityConfigRepository.save(config);
    }

    public boolean canVolunteerAcceptMore(Integer roundId, Integer volunteerId) {
        RoundCapacityConfig config = getOrCreateConfig(roundId);
        long currentOrders = orderAssignmentRepository
                .countActiveOrdersForVolunteerInRound(volunteerId, roundId);
        return currentOrders < config.getMaxOrdersPerVolunteer();
    }

    public RoundCapacityInfo getRoundCapacityInfo(Integer roundId) {
        RoundCapacityConfig config = getOrCreateConfig(roundId);
        long volunteerCount = roundSignupRepository.countConfirmedVolunteersForRound(roundId);
        long totalOrders = orderAssignmentRepository.countActiveOrdersForRound(roundId);

        int maxCapacity = config.getOverrideCapacity() != null ?
                config.getOverrideCapacity() :
                (int)(volunteerCount * config.getMaxOrdersPerVolunteer());

        return new RoundCapacityInfo(
                roundId,
                volunteerCount,
                totalOrders,
                maxCapacity,
                maxCapacity - (int)totalOrders,
                config.getMaxOrdersPerVolunteer()
        );
    }

    public static class RoundCapacityInfo {
        public final Integer roundId;
        public final Long volunteerCount;
        public final Long totalOrders;
        public final Integer maxCapacity;
        public final Integer availableSlots;
        public final Integer maxOrdersPerVolunteer;

        public RoundCapacityInfo(Integer roundId, Long volunteerCount, Long totalOrders,
                                 Integer maxCapacity, Integer availableSlots, Integer maxOrdersPerVolunteer) {
            this.roundId = roundId;
            this.volunteerCount = volunteerCount;
            this.totalOrders = totalOrders;
            this.maxCapacity = maxCapacity;
            this.availableSlots = availableSlots;
            this.maxOrdersPerVolunteer = maxOrdersPerVolunteer;
        }
    }
}