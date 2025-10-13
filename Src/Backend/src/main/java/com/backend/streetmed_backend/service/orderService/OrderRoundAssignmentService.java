package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundsRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundSignupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OrderRoundAssignmentService {
    private static final Logger logger = LoggerFactory.getLogger(OrderRoundAssignmentService.class);
    private final OrderRepository orderRepository;
    private final RoundsRepository roundsRepository;
    private final RoundSignupRepository roundSignupRepository;

    // Maximum ratio of orders to volunteers per round
    private static final int MAX_ORDERS_PER_VOLUNTEER = 5;

    @Autowired
    public OrderRoundAssignmentService(OrderRepository orderRepository,
                                       RoundsRepository roundsRepository,
                                       RoundSignupRepository roundSignupRepository) {
        this.orderRepository = orderRepository;
        this.roundsRepository = roundsRepository;
        this.roundSignupRepository = roundSignupRepository;
    }

    /**
     * Assign unassigned orders to the closest upcoming rounds.
     * This method is also scheduled to run periodically to handle new orders
     * and rebalance assignments when rounds or volunteers change.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void assignOrdersToRounds() {
        logger.info("Starting order assignment process");

        // Get all unassigned orders, ordered by creation date (oldest first for priority)
        List<Order> unassignedOrders = orderRepository.findByRoundIdIsNullOrderByRequestTimeAsc();

        if (unassignedOrders.isEmpty()) {
            logger.info("No unassigned orders found");
            return;
        }

        logger.info("Found {} unassigned orders to process", unassignedOrders.size());

        // Get all upcoming rounds with capacity information
        List<Rounds> upcomingRounds = roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                LocalDateTime.now(), "SCHEDULED");

        if (upcomingRounds.isEmpty()) {
            logger.info("No upcoming rounds available for assignment");
            return;
        }

        // Process each unassigned order
        for (Order order : unassignedOrders) {
            assignOrderToOptimalRound(order, upcomingRounds);
        }

        // Now balance any rounds that may be overloaded
        rebalanceRoundAssignments(upcomingRounds);

        logger.info("Order assignment process completed");
    }

    /**
     * Assign a single order to the optimal round based on timing and capacity
     */
    @Transactional
    public void assignOrderToOptimalRound(Order order, List<Rounds> availableRounds) {
        if (availableRounds.isEmpty()) {
            logger.info("No rounds available for order {}", order.getOrderId());
            return;
        }

        // Find the best round for this order
        Rounds optimalRound = findOptimalRound(order, availableRounds);

        if (optimalRound != null) {
            order.setRoundId(optimalRound.getRoundId());
            orderRepository.save(order);
            logger.info("Assigned order {} to round {}", order.getOrderId(), optimalRound.getRoundId());
        } else {
            logger.info("No suitable round found for order {}, keeping as pending", order.getOrderId());
        }
    }

    /**
     * Find the optimal round for an order based on capacity and timing
     */
    private Rounds findOptimalRound(Order order, List<Rounds> rounds) {
        // Check each round in chronological order
        for (Rounds round : rounds) {
            // Calculate current volunteer count for this round
            long confirmedVolunteers = roundSignupRepository.countConfirmedVolunteersForRound(round.getRoundId());

            // Skip rounds with no volunteers
            if (confirmedVolunteers == 0) {
                continue;
            }

            // Count current orders assigned to this round
            long currentOrderCount = orderRepository.countByRoundId(round.getRoundId());

            // Calculate maximum capacity based on volunteer count
            long maxCapacity = confirmedVolunteers * MAX_ORDERS_PER_VOLUNTEER;

            // If this round has capacity, use it
            if (currentOrderCount < maxCapacity) {
                return round;
            }
        }

        // If we get here, no round has capacity
        return null;
    }

    /**
     * Rebalance order assignments across rounds to optimize distribution
     */
    @Transactional
    protected void rebalanceRoundAssignments(List<Rounds> rounds) {
        if (rounds.isEmpty()) {
            return;
        }

        logger.info("Starting round assignment rebalancing");

        // Check for overloaded rounds
        for (int i = 0; i < rounds.size(); i++) {
            Rounds currentRound = rounds.get(i);

            // Calculate capacity
            long confirmedVolunteers = roundSignupRepository.countConfirmedVolunteersForRound(currentRound.getRoundId());
            long maxCapacity = confirmedVolunteers * MAX_ORDERS_PER_VOLUNTEER;

            // Get all orders for this round
            List<Order> assignedOrders = orderRepository.findByRoundId(currentRound.getRoundId());

            // If overloaded, try to move excess orders to later rounds
            if (assignedOrders.size() > maxCapacity) {
                // Sort orders by creation date (newest first, as they are lower priority)
                assignedOrders.sort(Comparator.comparing(Order::getRequestTime).reversed());

                // Calculate how many orders need to be moved
                int excessOrders = (int) (assignedOrders.size() - maxCapacity);

                // Try to find next rounds with capacity
                List<Rounds> laterRounds = rounds.subList(i + 1, rounds.size());

                // Process the excess orders
                for (int j = 0; j < excessOrders && j < assignedOrders.size(); j++) {
                    Order orderToMove = assignedOrders.get(j);

                    // Try to find a round with capacity
                    Rounds targetRound = findOptimalRound(orderToMove, laterRounds);

                    if (targetRound != null) {
                        // Move the order
                        orderToMove.setRoundId(targetRound.getRoundId());
                        orderRepository.save(orderToMove);
                        logger.info("Rebalanced order {} from round {} to round {}",
                                orderToMove.getOrderId(), currentRound.getRoundId(), targetRound.getRoundId());
                    } else {
                        // If no round has capacity, leave this order where it is
                        logger.info("No available round with capacity to rebalance order {}", orderToMove.getOrderId());
                    }
                }
            }
        }

        logger.info("Round assignment rebalancing completed");
    }

    /**
     * Handle a volunteer cancellation by rebalancing affected rounds
     */
    @Transactional
    public void handleVolunteerCancellation(Integer roundId) {
        logger.info("Handling volunteer cancellation for round {}", roundId);

        // Get all upcoming rounds
        List<Rounds> upcomingRounds = roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                LocalDateTime.now(), "SCHEDULED");

        // Find the affected round
        Optional<Rounds> affectedRoundOpt = upcomingRounds.stream()
                .filter(r -> r.getRoundId().equals(roundId))
                .findFirst();

        if (affectedRoundOpt.isPresent()) {
            // Rebalance this round and all future rounds
            int indexOfAffectedRound = upcomingRounds.indexOf(affectedRoundOpt.get());
            List<Rounds> affectedRounds = upcomingRounds.subList(indexOfAffectedRound, upcomingRounds.size());

            rebalanceRoundAssignments(affectedRounds);
        }
    }
}