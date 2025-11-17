package com.backend.streetmed_backend.repository.User;

import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerSubRoleRepository extends JpaRepository<VolunteerSubRole, Integer> {

    // Find sub-roles for a specific user
    List<VolunteerSubRole> findByUserId(Integer userId);

    // Find specific sub-role for a user
    Optional<VolunteerSubRole> findByUserIdAndSubRole(Integer userId, VolunteerSubRole.SubRoleType subRole);

    // Check if a user has a specific sub-role
    boolean existsByUserIdAndSubRole(Integer userId, VolunteerSubRole.SubRoleType subRole);

    // Delete all sub-roles for a user
    void deleteByUserId(Integer userId);
}