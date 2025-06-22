package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.StudyPlan;
import Study.Assistant.Studia.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByUserOrderByDateAsc(User user);
    
    List<StudyPlan> findByUserAndType(User user, String type);
    
    @Query("SELECT sp FROM StudyPlan sp WHERE sp.user = :user AND sp.date >= :startDate AND sp.date <= :endDate ORDER BY sp.date, sp.startTime")
    List<StudyPlan> findByUserAndDateBetween(@Param("user") User user, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT sp FROM StudyPlan sp WHERE sp.user = :user AND sp.className = :className ORDER BY sp.date")
    List<StudyPlan> findByUserAndClassName(@Param("user") User user, @Param("className") String className);
    
    @Query("SELECT sp FROM StudyPlan sp WHERE sp.user = :user AND sp.repeatGroupId = :repeatGroupId ORDER BY sp.date")
    List<StudyPlan> findByUserAndRepeatGroupId(@Param("user") User user, @Param("repeatGroupId") String repeatGroupId);
    
    void deleteByUserAndId(User user, Long id);
}
