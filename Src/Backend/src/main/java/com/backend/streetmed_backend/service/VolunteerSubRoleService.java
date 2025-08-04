package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole; import com.backend.streetmed_backend.entity.user_entity.VolunteerSubRole.SubRoleType; import com.backend.streetmed_backend.repository.User.VolunteerSubRoleRepository; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; import java.time.LocalDateTime; import java.util.List; import java.util.Optional;

@Service public class VolunteerSubRoleService {
    private final VolunteerSubRoleRepository volunteerSubRoleRepository;

    @Autowired
    public VolunteerSubRoleService(VolunteerSubRoleRepository volunteerSubRoleRepository) {
        this.volunteerSubRoleRepository = volunteerSubRoleRepository;
    }

    /**
     * Retrieves the first volunteer sub role for a given user.
     * If the user has multiple sub roles, the first one in the list is returned.
     * @param userId the ID of the user
     * @return an Optional containing the volunteer sub role if found, otherwise empty
     */
    public Optional<VolunteerSubRole> getVolunteerSubRole(Integer userId) {
        List<VolunteerSubRole> roles = volunteerSubRoleRepository.findByUserId(userId);
        if (roles.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(roles.get(0));
    }

    /**
     * Retrieves all volunteer sub roles for a given user.
     * @param userId the ID of the user
     * @return a list of volunteer sub roles
     */
    public List<VolunteerSubRole> getVolunteerSubRoles(Integer userId) {
        return volunteerSubRoleRepository.findByUserId(userId);
    }

    /**
     * Assigns or updates a volunteer sub role for the specified user.
     * If the user already has the given sub role, it updates the record;
     * otherwise, it creates a new one.
     * @param userId the ID of the user
     * @param subRole the sub role to assign (CLINICIAN, TEAM_LEAD, or REGULAR)
     * @param assignedBy the ID of the admin assigning this role
     * @param notes optional notes regarding the assignment
     * @return the saved VolunteerSubRole entity
     */
    public VolunteerSubRole assignVolunteerSubRole(Integer userId, SubRoleType subRole, Integer assignedBy, String notes) {
        Optional<VolunteerSubRole> existing = volunteerSubRoleRepository.findByUserIdAndSubRole(userId, subRole);
        VolunteerSubRole volunteerSubRole;
        if (existing.isPresent()) {
            volunteerSubRole = existing.get();
        } else {
            volunteerSubRole = new VolunteerSubRole();
            volunteerSubRole.setUserId(userId);
        }
        volunteerSubRole.setSubRole(subRole);
        volunteerSubRole.setAssignedBy(assignedBy);
        volunteerSubRole.setNotes(notes);
        volunteerSubRole.setAssignedDate(LocalDateTime.now());
        return volunteerSubRoleRepository.save(volunteerSubRole);
    }

    /**
     * Removes all volunteer sub role records for the given user.
     * @param userId the ID of the user
     */
    public void removeVolunteerSubRoles(Integer userId) {
        volunteerSubRoleRepository.deleteByUserId(userId);
    }

    /**
     * Checks if the user has a specific volunteer sub role.
     * @param userId the ID of the user
     * @param subRole the sub role to check for
     * @return true if the user has the specified sub role; otherwise, false
     */
    public boolean hasVolunteerSubRole(Integer userId, SubRoleType subRole) {
        return volunteerSubRoleRepository.existsByUserIdAndSubRole(userId, subRole);
    }

}