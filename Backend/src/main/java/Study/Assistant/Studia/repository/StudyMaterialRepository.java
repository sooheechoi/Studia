package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByUserId(Long userId);
    List<StudyMaterial> findByUserIdAndCourseId(Long userId, Long courseId);
    List<StudyMaterial> findByCourseId(Long courseId);
}
