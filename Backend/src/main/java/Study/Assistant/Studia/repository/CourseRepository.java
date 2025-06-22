package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserId(Long userId);
    
    @Query("SELECT c FROM Course c WHERE c.user.id = :userId")
    List<Course> findCoursesForUser(@Param("userId") Long userId);
}
