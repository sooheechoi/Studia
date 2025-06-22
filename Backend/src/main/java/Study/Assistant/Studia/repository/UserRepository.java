package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT DISTINCT u FROM User u JOIN u.courses c WHERE c.id = :courseId")
    List<User> findUsersByCourse(@Param("courseId") Long courseId);
    
    List<User> findByCreatedAtAfter(LocalDateTime dateTime);
    
    // For backward compatibility with Date parameter
    default List<User> findByCreatedAtAfter(Date date) {
        LocalDateTime dateTime = new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        return findByCreatedAtAfter(dateTime);
    }
}
