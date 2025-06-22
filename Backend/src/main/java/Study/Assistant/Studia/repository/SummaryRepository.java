package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    List<Summary> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Summary> findByUserId(Long userId);
}
