package com.backend.streetmed_backend.scheduler;

import com.backend.streetmed_backend.service.OrderRoundAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class OrderAssignmentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OrderAssignmentScheduler.class);
    private final OrderRoundAssignmentService orderRoundAssignmentService;

    @Autowired
    public OrderAssignmentScheduler(OrderRoundAssignmentService orderRoundAssignmentService) {
        this.orderRoundAssignmentService = orderRoundAssignmentService;
    }

    // Run every hour (3600000 milliseconds)
    @Scheduled(fixedRate = 3600000)
    public void assignOrdersToRounds() {
        logger.info("Starting scheduled order assignment to rounds");
        try {
            orderRoundAssignmentService.assignOrdersToRounds();
            logger.info("Completed scheduled order assignment to rounds");
        } catch (Exception e) {
            logger.error("Error during scheduled order assignment: {}", e.getMessage(), e);
        }
    }
}