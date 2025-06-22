package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStudyMaterialId(Long studyMaterialId);
    
    @Query("SELECT q FROM Quiz q WHERE q.studyMaterial.user.id = :userId")
    List<Quiz> findByStudyMaterial_UserId(@Param("userId") Long userId);
    
    @Query("SELECT q FROM Quiz q WHERE q.studyMaterial.id = :materialId AND q.studyMaterial.user.id = :userId")
    List<Quiz> findByStudyMaterial_IdAndStudyMaterial_UserId(@Param("materialId") Long materialId, @Param("userId") Long userId);
}
