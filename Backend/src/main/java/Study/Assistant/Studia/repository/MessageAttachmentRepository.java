package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.GroupMessage;
import Study.Assistant.Studia.domain.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    List<MessageAttachment> findByMessage(GroupMessage message);
    void deleteByMessage(GroupMessage message);
}
