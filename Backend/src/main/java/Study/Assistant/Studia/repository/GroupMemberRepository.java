package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.GroupMember;
import Study.Assistant.Studia.domain.entity.StudyGroup;
import Study.Assistant.Studia.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    // Find membership by user and group
    Optional<GroupMember> findByStudyGroupAndUser(StudyGroup studyGroup, User user);
    
    // Find all members of a group
    List<GroupMember> findByStudyGroupAndStatus(StudyGroup studyGroup, GroupMember.Status status);
    
    // Find pending invitations for a user
    List<GroupMember> findByUserAndStatus(User user, GroupMember.Status status);
    
    // Count pending invitations for a user
    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.user = :user AND m.status = 'PENDING'")
    Long countPendingInvitations(@Param("user") User user);
    
    // Check if user is member of group
    @Query("SELECT COUNT(m) > 0 FROM GroupMember m WHERE m.studyGroup = :group AND m.user = :user AND m.status = 'ACTIVE'")
    boolean isMember(@Param("group") StudyGroup group, @Param("user") User user);
    
    // Get user's role in a group
    @Query("SELECT m.role FROM GroupMember m WHERE m.studyGroup = :group AND m.user = :user AND m.status = 'ACTIVE'")
    Optional<GroupMember.Role> getUserRole(@Param("group") StudyGroup group, @Param("user") User user);
}
