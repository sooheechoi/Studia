package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.StudyGroup;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.domain.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    
    // Find all public groups
    List<StudyGroup> findByIsPublicTrueAndIsActiveTrue();
    
    // Find groups by course
    List<StudyGroup> findByCourseAndIsActiveTrue(Course course);
    
    // Find groups owned by a user
    List<StudyGroup> findByOwnerAndIsActiveTrue(User owner);
    
    // Find groups where user is a member
    @Query("SELECT g FROM StudyGroup g JOIN g.members m WHERE m.user = :user AND m.status = 'ACTIVE' AND g.isActive = true")
    List<StudyGroup> findUserGroups(@Param("user") User user);
    
    // Search groups by name or description
    @Query("SELECT g FROM StudyGroup g WHERE g.isActive = true AND g.isPublic = true AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<StudyGroup> searchGroups(@Param("query") String query);
    
    // Count active members in a group
    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.studyGroup = :group AND m.status = 'ACTIVE'")
    Long countActiveMembers(@Param("group") StudyGroup group);
    
    // Find groups with available space
    @Query("SELECT g FROM StudyGroup g WHERE g.isActive = true AND g.isPublic = true AND SIZE(g.members) < g.maxMembers")
    List<StudyGroup> findAvailableGroups();
}
