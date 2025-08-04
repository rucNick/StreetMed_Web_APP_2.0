package com.backend.streetmed_backend.repository.Order;

import com.backend.streetmed_backend.entity.order_entity.Order;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems")
    List<Order> findAllWithItems();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") Integer userId);

    List<Order> findByStatus(String status);
    List<Order> findByUserId(Integer userId);

    List<Order> findByRoundIdIsNull();
    List<Order> findByRoundId(Integer roundId);

    // Add to OrderRepository.java
    List<Order> findByRoundIdIsNullOrderByRequestTimeAsc();
    long countByRoundId(Integer roundId);

    List<Order> findByVolunteerId(Integer volunteerId);

    List<Order> findByVolunteerIdAndStatus(Integer volunteerId, String status);
    List<Order> findByVolunteerIdAndStatusIn(Integer volunteerId, List<String> statuses);
}