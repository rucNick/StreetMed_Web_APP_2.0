package com.backend.streetmed_backend.repository.Order;

import com.backend.streetmed_backend.entity.order_entity.OrderAssignment;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Integer> {

    // Find active assignment for an order
    @Query("SELECT a FROM OrderAssignment a WHERE a.orderId = :orderId " +
            "AND a.status NOT IN ('CANCELLED') " +
            "ORDER BY a.acceptedAt DESC")
    Optional<OrderAssignment> findActiveAssignmentForOrder(@Param("orderId") Integer orderId);

    // Find by volunteer and multiple statuses
    List<OrderAssignment> findByVolunteerIdAndStatusIn(Integer volunteerId, List<AssignmentStatus> statuses);

    // Find active assignments for volunteer in round
    @Query("SELECT a FROM OrderAssignment a WHERE a.roundId = :roundId " +
            "AND a.volunteerId = :volunteerId " +
            "AND a.status IN ('ACCEPTED', 'IN_PROGRESS')")
    List<OrderAssignment> findActiveAssignmentsForVolunteerInRound(@Param("roundId") Integer roundId,
                                                                   @Param("volunteerId") Integer volunteerId);

    @Query("SELECT a FROM OrderAssignment a WHERE a.orderId = :orderId AND a.volunteerId = :volunteerId")
    Optional<OrderAssignment> findByOrderIdAndVolunteerId(@Param("orderId") Integer orderId, @Param("volunteerId") Integer volunteerId);

    // Count by round, volunteer and statuses
    long countByRoundIdAndVolunteerIdAndStatusIn(Integer roundId, Integer volunteerId, List<AssignmentStatus> statuses);

    // NEW METHODS for RoundCapacityService
    @Query("SELECT COUNT(a) FROM OrderAssignment a " +
            "WHERE a.volunteerId = :volunteerId AND a.roundId = :roundId " +
            "AND a.status IN ('ACCEPTED', 'IN_PROGRESS')")
    long countActiveOrdersForVolunteerInRound(@Param("volunteerId") Integer volunteerId,
                                              @Param("roundId") Integer roundId);

    @Query("SELECT COUNT(a) FROM OrderAssignment a " +
            "WHERE a.roundId = :roundId " +
            "AND a.status IN ('ACCEPTED', 'IN_PROGRESS')")
    long countActiveOrdersForRound(@Param("roundId") Integer roundId);

    List<OrderAssignment> findByVolunteerId(Integer volunteerId);
}