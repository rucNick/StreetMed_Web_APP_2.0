package com.backend.streetmed_backend.repository;

import com.backend.streetmed_backend.entity.Service_entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByNameContainingIgnoreCase(String name);
    List<Feedback> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Feedback> findByOrderByCreatedAtDesc();
}