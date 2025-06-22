package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.GroupMessage;
import Study.Assistant.Studia.domain.entity.StudyGroup;
import Study.Assistant.Studia.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    
    // Find messages by group (paginated)
    Page<GroupMessage> findByStudyGroupAndIsDeletedFalseOrderBySentAtDesc(StudyGroup studyGroup, Pageable pageable);
    
    // Find recent messages for a group
    List<GroupMessage> findTop50ByStudyGroupAndIsDeletedFalseOrderBySentAtDesc(StudyGroup studyGroup);
    
    // Find messages sent after a certain time
    List<GroupMessage> findByStudyGroupAndSentAtAfterAndIsDeletedFalseOrderBySentAt(StudyGroup studyGroup, LocalDateTime after);
    
    // Count unread messages (simplified - in real app, you'd track read status per user)
    @Query("SELECT COUNT(m) FROM GroupMessage m WHERE m.studyGroup = :group AND m.sentAt > :lastRead AND m.sender != :user AND m.isDeleted = false")
    Long countUnreadMessages(@Param("group") StudyGroup group, @Param("lastRead") LocalDateTime lastRead, @Param("user") User user);
    
    // Get recent messages across all user's groups
    @Query("SELECT m FROM GroupMessage m JOIN m.studyGroup g JOIN g.members gm WHERE gm.user = :user AND gm.status = 'ACTIVE' AND m.isDeleted = false ORDER BY m.sentAt DESC")
    List<GroupMessage> findRecentMessagesForUser(@Param("user") User user, Pageable pageable);
}
