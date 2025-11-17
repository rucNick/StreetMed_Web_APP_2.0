package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderRoundAssignmentService {
    private static final Logger logger = LoggerFactory.getLogger(OrderRoundAssignmentService.class);

    private final OrderRepository orderRepository;
    private final RoundsRepository roundsRepository;

    @Autowired
    public OrderRoundAssignmentService(OrderRepository orderRepository,
                                       RoundsRepository roundsRepository) {
        this.orderRepository = orderRepository;
        this.roundsRepository = roundsRepository;
    }

    /**
     * Assigns an order to the most optimal round based on capacity
     */
    @Transactional
    public void assignOrderToOptimalRound(Order order, List<Rounds> upcomingRounds) {
        if (upcomingRounds.isEmpty()) {
            logger.info("No upcoming rounds available for order {}", order.getOrderId());
            return;
        }

        // Find the first round with available capacity
        for (Rounds round : upcomingRounds) {
            if (canAssignOrderToRound(round)) {
                order.setRoundId(round.getRoundId());
                orderRepository.save(order);
                logger.info("Assigned order {} to round {}", order.getOrderId(), round.getRoundId());
                return;
            }
        }

        logger.info("No rounds with available capacity for order {}", order.getOrderId());
    }

    /**
     * Checks if a round has capacity for more orders
     */
    public boolean canAssignOrderToRound(Rounds round) {
        // Get current order count for this round
        long currentOrderCount = orderRepository.countByRoundId(round.getRoundId());

        // Get the round's order capacity (default to 20 if not set)
        Integer orderCapacity = round.getOrderCapacity() != null ? round.getOrderCapacity() : 20;

        return currentOrderCount < orderCapacity;
    }

    /**
     * Rebalances orders when a volunteer cancels their round signup
     * This redistributes orders from rounds that may now lack volunteers
     */
    @Transactional
    public void handleVolunteerCancellation(Integer roundId) {
        try {
            // Get the round
            Rounds round = roundsRepository.findById(roundId)
                    .orElseThrow(() -> new RuntimeException("Round not found"));

            // Check if the round still has enough volunteers
            // If not, we might need to reassign some orders
            // For now, just log it - you can add more complex logic here
            logger.info("Handling volunteer cancellation for round {}", roundId);

            // Optional: Redistribute orders if needed
            // This would involve checking volunteer count vs order count
            // and potentially moving orders to other rounds

        } catch (Exception e) {
            logger.error("Error handling volunteer cancellation: {}", e.getMessage());
        }
    }

    /**
     * Gets the current order count for a round
     */
    public long getOrderCountForRound(Integer roundId) {
        return orderRepository.countByRoundId(roundId);
    }

    /**
     * Reassigns unassigned orders to available rounds
     * Useful for batch processing or admin operations
     */
    @Transactional
    public int assignUnassignedOrders() {
        // Get all unassigned orders (roundId is null)
        List<Order> unassignedOrders = orderRepository.findByRoundIdIsNullAndStatus("PENDING");

        // Get upcoming rounds with capacity
        List<Rounds> upcomingRounds = roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                LocalDateTime.now(), "SCHEDULED");

        int assignedCount = 0;

        for (Order order : unassignedOrders) {
            for (Rounds round : upcomingRounds) {
                if (canAssignOrderToRound(round)) {
                    order.setRoundId(round.getRoundId());
                    orderRepository.save(order);
                    assignedCount++;
                    logger.info("Assigned unassigned order {} to round {}", order.getOrderId(), round.getRoundId());
                    break;
                }
            }
        }

        logger.info("Assigned {} previously unassigned orders to rounds", assignedCount);
        return assignedCount;
    }

    /**
     * Removes orders from a cancelled round
     */
    @Transactional
    public void handleRoundCancellation(Integer roundId) {
        // Get all orders assigned to this round
        List<Order> roundOrders = orderRepository.findByRoundId(roundId);

        // Unassign them from the round
        for (Order order : roundOrders) {
            order.setRoundId(null);
            orderRepository.save(order);
        }

        logger.info("Unassigned {} orders from cancelled round {}", roundOrders.size(), roundId);

        // Try to reassign them to other rounds
        List<Rounds> upcomingRounds = roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                LocalDateTime.now(), "SCHEDULED");

        for (Order order : roundOrders) {
            assignOrderToOptimalRound(order, upcomingRounds);
        }
    }
}