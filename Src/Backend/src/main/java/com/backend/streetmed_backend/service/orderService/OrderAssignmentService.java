package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment.AssignmentStatus;
import com.backend.streetmed_backend.repository.Order.OrderAssignmentRepository;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderAssignmentService {
    private static final Logger logger = LoggerFactory.getLogger(OrderAssignmentService.class);

    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderAssignmentService(OrderAssignmentRepository orderAssignmentRepository,
                                  OrderRepository orderRepository) {
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Accept an order for a volunteer
     */
    @Transactional
    public OrderAssignment acceptOrder(Integer orderId, Integer volunteerId, Integer roundId) {
        // Check if order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if this volunteer already has an assignment for this order
        Optional<OrderAssignment> existingVolunteerAssignment =
                orderAssignmentRepository.findByOrderIdAndVolunteerId(orderId, volunteerId);

        if (existingVolunteerAssignment.isPresent()) {
            OrderAssignment existing = existingVolunteerAssignment.get();

            // If assignment exists and is not cancelled, just return it and update order status
            if (existing.getStatus() != AssignmentStatus.CANCELLED) {
                logger.info("Volunteer {} already has active assignment for order {}, updating order status",
                        volunteerId, orderId);

                // Make sure the order status is updated to match the assignment
                if ("PENDING".equals(order.getStatus())) {
                    order.setStatus("ACCEPTED");
                    orderRepository.save(order);
                }

                return existing; // Return existing assignment
            }

            // If it was cancelled, reactivate it
            logger.info("Reactivating cancelled assignment for order {} by volunteer {}", orderId, volunteerId);
            existing.setStatus(AssignmentStatus.ACCEPTED);
            existing.setAcceptedAt(LocalDateTime.now());
            existing.setRoundId(roundId);

            // Update order status
            order.setStatus("ACCEPTED");
            orderRepository.save(order);

            return orderAssignmentRepository.save(existing);
        }

        // Check if order is already assigned to someone else
        Optional<OrderAssignment> activeAssignment =
                orderAssignmentRepository.findActiveAssignmentForOrder(orderId);

        if (activeAssignment.isPresent() && !activeAssignment.get().getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("ORDER_ALREADY_ACCEPTED: This order was already accepted by another volunteer");
        }

        // Only create new assignment if none exists
        OrderAssignment assignment = new OrderAssignment(orderId, volunteerId);
        assignment.setRoundId(roundId);
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());

        // Save assignment
        OrderAssignment savedAssignment = orderAssignmentRepository.save(assignment);

        // Update order status
        order.setStatus("ACCEPTED");
        orderRepository.save(order);

        logger.info("Order {} accepted by volunteer {} for round {}", orderId, volunteerId, roundId);

        return savedAssignment;
    }

    /**
     * Start working on an order (change status to IN_PROGRESS)
     */
    @Transactional
    public OrderAssignment startOrder(Integer assignmentId, Integer volunteerId) {
        OrderAssignment assignment = orderAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Verify volunteer owns this assignment
        if (!assignment.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("Unauthorized: Assignment belongs to another volunteer");
        }

        // Verify status transition is valid
        if (!assignment.isAccepted()) {
            throw new RuntimeException("Invalid status transition: Order must be ACCEPTED before starting");
        }

        assignment.setStatus(AssignmentStatus.IN_PROGRESS);

        // Update order status
        Order order = orderRepository.findById(assignment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus("PROCESSING");
        orderRepository.save(order);

        return orderAssignmentRepository.save(assignment);
    }

    /**
     * Complete an order
     */
    @Transactional
    public OrderAssignment completeOrder(Integer assignmentId, Integer volunteerId) {
        OrderAssignment assignment = orderAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!assignment.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("Unauthorized: Assignment belongs to another volunteer");
        }

        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only complete orders that are in progress");
        }

        // Update assignment status
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());

        // Update the order status AND set the volunteer ID
        Order order = orderRepository.findById(assignment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus("COMPLETED");
        order.setDeliveryTime(LocalDateTime.now());
        order.setAssignedVolunteerId(volunteerId);
        orderRepository.save(order);

        return orderAssignmentRepository.save(assignment);
    }

    /**
     * Cancel an assignment
     */
    @Transactional
    public OrderAssignment cancelAssignment(Integer assignmentId, Integer volunteerId) {
        OrderAssignment assignment = orderAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Verify volunteer owns this assignment
        if (!assignment.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("Unauthorized: Assignment belongs to another volunteer");
        }

        // Can't cancel completed assignments
        if (assignment.isCompleted()) {
            throw new RuntimeException("Cannot cancel completed assignment");
        }

        assignment.setStatus(AssignmentStatus.CANCELLED);

        // Update order status back to PENDING
        Order order = orderRepository.findById(assignment.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus("PENDING");
        orderRepository.save(order);

        logger.info("Assignment {} cancelled by volunteer {}", assignmentId, volunteerId);

        return orderAssignmentRepository.save(assignment);
    }

    /**
     * Get active assignments for a volunteer
     */
    @Transactional(readOnly = true)
    public List<OrderAssignment> getActiveAssignments(Integer volunteerId) {
        List<AssignmentStatus> activeStatuses = Arrays.asList(
                AssignmentStatus.PENDING_ACCEPT,
                AssignmentStatus.ACCEPTED,
                AssignmentStatus.IN_PROGRESS
        );

        return orderAssignmentRepository.findByVolunteerIdAndStatusIn(volunteerId, activeStatuses);
    }

    /**
     * Get assignments for a volunteer in a specific round
     */
    @Transactional(readOnly = true)
    public List<OrderAssignment> getVolunteerRoundAssignments(Integer volunteerId, Integer roundId) {
        return orderAssignmentRepository.findActiveAssignmentsForVolunteerInRound(roundId, volunteerId);
    }

    /**
     * Count active orders for a volunteer in a round
     */
    @Transactional(readOnly = true)
    public long countVolunteerOrdersInRound(Integer volunteerId, Integer roundId) {
        List<AssignmentStatus> activeStatuses = Arrays.asList(
                AssignmentStatus.ACCEPTED,
                AssignmentStatus.IN_PROGRESS
        );

        return orderAssignmentRepository.countByRoundIdAndVolunteerIdAndStatusIn(
                roundId, volunteerId, activeStatuses);
    }

    /**
     * Get assignment for an order
     */
    @Transactional(readOnly = true)
    public Optional<OrderAssignment> getOrderAssignment(Integer orderId) {
        return orderAssignmentRepository.findActiveAssignmentForOrder(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderAssignment> getAllAssignments(Integer volunteerId) {
        return orderAssignmentRepository.findByVolunteerId(volunteerId);
    }
}