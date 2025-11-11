package com.backend.streetmed_backend.service.orderService;
import com.backend.streetmed_backend.entity.CargoItem;
import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment.AssignmentStatus;
import com.backend.streetmed_backend.entity.order_entity.OrderItem;
import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import com.backend.streetmed_backend.repository.Order.OrderAssignmentRepository;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import com.backend.streetmed_backend.repository.Rounds.RoundsRepository;
import com.backend.streetmed_backend.repository.User.UserRepository;
import com.backend.streetmed_backend.service.cargoService.CargoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CargoItemService cargoItemService;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private static final int GUEST_USER_ID = -1;
    private final RoundsRepository roundsRepository;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(OrderService.class.getName());
    private final OrderRateLimitService rateLimitService;

    @Autowired
    private OrderRoundAssignmentService orderRoundAssignmentService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CargoItemService cargoItemService,
                        RoundsRepository roundsRepository,
                        OrderAssignmentService orderAssignmentService,
                        OrderAssignmentRepository orderAssignmentRepository,
                        OrderRateLimitService rateLimitService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cargoItemService = cargoItemService;
        this.roundsRepository = roundsRepository;
        this.orderAssignmentService = orderAssignmentService;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Get orders for a specific round
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersForRound(Integer roundId) {
        List<Order> orders = orderRepository.findByRoundId(roundId);
        orders.forEach(order -> order.getOrderItems().size()); // Force initialization
        return orders;
    }

    /**
     * Manually assign an order to a round
     */
    @Transactional
    public Order assignOrderToRound(Integer orderId, Integer roundId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (roundId != null) {
            Rounds round = roundsRepository.findById(roundId)
                    .orElseThrow(() -> new RuntimeException("Round not found"));

            // Check if round is in the future
            if (round.getStartTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Cannot assign orders to past rounds");
            }
        }

        order.setRoundId(roundId);
        return orderRepository.save(order);
    }

    /**
     * Creates a new order and reserves the associated inventory items.
     *
     * @param order The order to create
     * @param items The items in the order
     * @param clientIpAddress The client's IP address for rate limiting
     * @return The saved order
     */
    @Transactional
    public Order createOrder(Order order, List<OrderItem> items, String clientIpAddress) {

        // Rate limiting checks
        if (order.getUserId() != GUEST_USER_ID) {
            // Check rate limits for registered user
            validateUser(order.getUserId());
            rateLimitService.checkUserRateLimit(order.getUserId());
        } else {
            // Check rate limits for guest (IP-based)
            if (clientIpAddress == null || clientIpAddress.trim().isEmpty()) {
                throw new IllegalArgumentException("IP address is required for guest orders");
            }
            rateLimitService.checkGuestRateLimit(clientIpAddress);
        }

        // Set IP address for tracking
        order.setClientIpAddress(clientIpAddress);
        order.setRequestTime(LocalDateTime.now());
        order.setStatus("PENDING");

        if (items.isEmpty()) {
            throw new RuntimeException("Order must contain at least one item");
        }

        // Reserve inventory items (decrement quantities temporarily)
        Map<String, Integer> itemQuantityMap = new HashMap<>();
        for (OrderItem item : items) {
            String itemName = item.getItemName();
            int quantity = item.getQuantity();

            // Aggregate quantities for items that might appear multiple times
            itemQuantityMap.put(itemName,
                    itemQuantityMap.getOrDefault(itemName, 0) + quantity);
        }

        // Check inventory availability and reserve items
        for (Map.Entry<String, Integer> entry : itemQuantityMap.entrySet()) {
            String itemName = entry.getKey();
            int requestedQuantity = entry.getValue();

            // Find the cargo item by name
            List<CargoItem> matchingItems = cargoItemService.searchItems(itemName);
            if (matchingItems.isEmpty()) {
                throw new RuntimeException("Item not found: " + itemName);
            }

            // Use the first matching item (assuming item names are unique)
            CargoItem cargoItem = matchingItems.get(0);

            // Check if there's enough inventory
            if (!cargoItem.isAvailableInQuantity(requestedQuantity)) {
                throw new RuntimeException("Insufficient quantity available for: " + itemName);
            }

            // Reserve the inventory (temporarily reduce quantity)
            cargoItemService.reserveItems(cargoItem.getId(), requestedQuantity);
        }

        // Set summary information
        order.setItemName(items.size() + " items"); // e.g. "3 items"
        order.setQuantity(items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum()); // Total quantity

        // Set up bidirectional relationship
        for (OrderItem item : items) {
            order.addOrderItem(item);
        }

        // Save the order first
        Order savedOrder = orderRepository.save(order);

        // Record for rate limiting
        rateLimitService.recordOrderCreation(
                order.getUserId() != GUEST_USER_ID ? order.getUserId() : null,
                clientIpAddress,
                savedOrder.getOrderId()
        );

        // Try to assign it to a round immediately
        try {
            List<Rounds> upcomingRounds = roundsRepository.findByStartTimeAfterAndStatusOrderByStartTimeAsc(
                    LocalDateTime.now(), "SCHEDULED");
            orderRoundAssignmentService.assignOrderToOptimalRound(savedOrder, upcomingRounds);

            // If assignment was successful, reload the order to get updated roundId
            savedOrder = orderRepository.findById(savedOrder.getOrderId()).orElse(savedOrder);
        } catch (Exception e) {
            // Log error but don't fail the order creation
            logger.severe("Failed to assign order to round: " + e.getMessage());
            logger.throwing(OrderService.class.getName(), "createOrder", e);
        }

        return savedOrder;
    }

    /**
     * Get a specific order with permission checking
     */
    public Order getOrder(Integer orderId, Integer userId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Allow access to guest orders for volunteers only
        if (order.getUserId() == GUEST_USER_ID) {
            if (!"VOLUNTEER".equals(userRole)) {
                throw new RuntimeException("Unauthorized access to order");
            }
            return order;
        }

        // For regular orders: only allow volunteers to view any order, clients can only view their own orders
        if (!"VOLUNTEER".equals(userRole) && !order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }

        return order;
    }

    /**
     * Get all orders in the system
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        // Initialize the collections
        orders.forEach(order -> order.getOrderItems().size()); // Force initialization
        return orders;
    }

    /**
     * Get orders for a user (or all orders if volunteer)
     */
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Integer userId, String userRole) {
        List<Order> orders;
        if ("VOLUNTEER".equals(userRole)) {
            orders = orderRepository.findAll();
        } else {
            orders = orderRepository.findByUserId(userId);
        }
        orders.forEach(order -> order.getOrderItems().size()); // Force initialization
        return orders;
    }

    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Updates the status of an order through OrderAssignment if exists
     */
    @Transactional
    public Order updateOrderStatus(Integer orderId, String status, Integer userId, String userRole) {
        if (!"VOLUNTEER".equals(userRole) && !"ADMIN".equals(userRole)) {
            throw new RuntimeException("Only volunteers and admins can update order status");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if there's an active assignment
        Optional<OrderAssignment> assignmentOpt = orderAssignmentService.getOrderAssignment(orderId);

        if (assignmentOpt.isPresent()) {
            OrderAssignment assignment = assignmentOpt.get();

            // Verify the volunteer owns this assignment (admins can update any order)
            if (!"ADMIN".equals(userRole) && !assignment.getVolunteerId().equals(userId)) {
                throw new RuntimeException("Order is assigned to another volunteer");
            }

            // Update through assignment service based on status
            // For admins updating orders they don't own, use the volunteer's ID from the assignment
            Integer effectiveUserId = "ADMIN".equals(userRole) ? assignment.getVolunteerId() : userId;

            switch (status) {
                case "PROCESSING":
                    orderAssignmentService.startOrder(assignment.getAssignmentId(), effectiveUserId);
                    break;
                case "COMPLETED":
                    orderAssignmentService.completeOrder(assignment.getAssignmentId(), effectiveUserId);
                    break;
                case "CANCELLED":
                    orderAssignmentService.cancelAssignment(assignment.getAssignmentId(), effectiveUserId);
                    releaseReservedInventory(order);
                    break;
                default:
                    // For other statuses, update directly
                    order.setStatus(status);
                    orderRepository.save(order);
            }

            // Reload order to get updated status
            order = orderRepository.findById(orderId).orElse(order);
        } else {
            // No assignment exists, update order directly (backward compatibility)
            String oldStatus = order.getStatus();
            order.setStatus(status);

            if (status.equals("COMPLETED") && !oldStatus.equals("COMPLETED")) {
                order.setDeliveryTime(LocalDateTime.now());
            } else if (status.equals("CANCELLED") && !oldStatus.equals("CANCELLED")) {
                releaseReservedInventory(order);
            }

            orderRepository.save(order);
        }

        return order;
    }

    /**
     * Cancels an order and releases any reserved inventory
     */
    @Transactional
    public void cancelOrder(Integer orderId, Integer userId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Allow admins to cancel any order
        if ("ADMIN".equals(userRole)) {
            // Admins can cancel any order
        } else if (order.getUserId() == GUEST_USER_ID) {
            // Only volunteers can cancel guest orders
            if (!"VOLUNTEER".equals(userRole)) {
                throw new RuntimeException("Unauthorized to cancel this order");
            }
        } else if (!"VOLUNTEER".equals(userRole) && !order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        // Check if there's an active assignment
        Optional<OrderAssignment> assignmentOpt = orderAssignmentService.getOrderAssignment(orderId);
        if (assignmentOpt.isPresent()) {
            // Cancel through assignment service
            OrderAssignment assignment = assignmentOpt.get();
            if (assignment.getVolunteerId().equals(userId) || "VOLUNTEER".equals(userRole) || "ADMIN".equals(userRole)) {
                orderAssignmentService.cancelAssignment(assignment.getAssignmentId(),
                        assignment.getVolunteerId());
            }
        }

        // Don't release inventory if order was already COMPLETED
        if (!order.getStatus().equals("COMPLETED")) {
            // Release the reserved inventory
            releaseReservedInventory(order);
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    /**
     * Deletes an order from the database (Admin only)
     * Should be called after cancelOrder to properly release inventory
     */
    @Transactional
    public void deleteOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Delete any associated assignments first
        Optional<OrderAssignment> assignmentOpt = orderAssignmentService.getOrderAssignment(orderId);
        if (assignmentOpt.isPresent()) {
            orderAssignmentRepository.delete(assignmentOpt.get());
        }

        // Delete the order
        orderRepository.delete(order);
    }

    /**
     * Get orders assigned to a specific volunteer through OrderAssignment
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByVolunteer(Integer volunteerId) {
        // Get all assignments for this volunteer
        List<OrderAssignment> assignments = orderAssignmentRepository
                .findByVolunteerId(volunteerId);

        List<Order> orders = new ArrayList<>();
        for (OrderAssignment assignment : assignments) {
            if (!assignment.isCancelled()) {
                Order order = orderRepository.findById(assignment.getOrderId()).orElse(null);
                if (order != null) {
                    order.getOrderItems().size(); // Force initialization
                    orders.add(order);
                }
            }
        }

        return orders;
    }

    /**
     * Get active orders (ACCEPTED or IN_PROGRESS) assigned to a specific volunteer
     */
    @Transactional(readOnly = true)
    public List<Order> getActiveOrdersByVolunteer(Integer volunteerId) {
        List<OrderAssignment> assignments = orderAssignmentService.getActiveAssignments(volunteerId);

        List<Order> orders = new ArrayList<>();
        for (OrderAssignment assignment : assignments) {
            if (assignment.isAccepted() || assignment.isInProgress()) {
                Order order = orderRepository.findById(assignment.getOrderId()).orElse(null);
                if (order != null) {
                    order.getOrderItems().size(); // Force initialization
                    orders.add(order);
                }
            }
        }

        // Sort by request time (newest first)
        orders.sort(Comparator.comparing(Order::getRequestTime).reversed());
        return orders;
    }

    /**
     * Get completed orders assigned to a specific volunteer
     */
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrdersByVolunteer(Integer volunteerId) {
        // Get all completed assignments
        List<OrderAssignment> assignments = orderAssignmentRepository
                .findByVolunteerIdAndStatus(volunteerId, AssignmentStatus.COMPLETED);

        List<Order> orders = new ArrayList<>();
        for (OrderAssignment assignment : assignments) {
            Order order = orderRepository.findById(assignment.getOrderId()).orElse(null);
            if (order != null) {
                order.getOrderItems().size(); // Force initialization
                orders.add(order);
            }
        }

        // Sort by delivery time (newest first)
        orders.sort((o1, o2) -> {
            if (o1.getDeliveryTime() == null && o2.getDeliveryTime() == null) return 0;
            if (o1.getDeliveryTime() == null) return 1;
            if (o2.getDeliveryTime() == null) return -1;
            return o2.getDeliveryTime().compareTo(o1.getDeliveryTime());
        });

        return orders;
    }

    /**
     * Releases inventory that was reserved for an order
     */
    private void releaseReservedInventory(Order order) {
        // Get all items in the order
        List<OrderItem> orderItems = order.getOrderItems();

        // Group items by name and sum quantities
        Map<String, Integer> itemQuantityMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            String itemName = item.getItemName();
            int quantity = item.getQuantity();

            itemQuantityMap.put(itemName,
                    itemQuantityMap.getOrDefault(itemName, 0) + quantity);
        }

        // Restore quantities to inventory
        for (Map.Entry<String, Integer> entry : itemQuantityMap.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();

            // Find the cargo item by name
            List<CargoItem> matchingItems = cargoItemService.searchItems(itemName);
            if (!matchingItems.isEmpty()) {
                CargoItem cargoItem = matchingItems.get(0);

                // Restore the quantity (add back to inventory)
                cargoItemService.updateQuantity(
                        cargoItem.getId(),
                        cargoItem.getQuantity() + quantity
                );
            }
        }
    }

    /**
     * Validate that a user exists
     */
    private void validateUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
    }
}