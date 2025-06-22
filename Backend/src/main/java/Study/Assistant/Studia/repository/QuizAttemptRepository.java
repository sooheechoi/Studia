package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    // 사용자의 특정 자료에 대한 모든 퀴즈 시도 조회
    @Query("SELECT qa FROM QuizAttempt qa " +
           "JOIN qa.quiz q " +
           "WHERE qa.user.id = :userId " +
           "AND q.studyMaterial.id = :materialId " +
           "ORDER BY qa.attemptedAt DESC")
    List<QuizAttempt> findByUserIdAndQuiz_StudyMaterial_Id(
            @Param("userId") Long userId, 
            @Param("materialId") Long materialId);
    
    // 사용자의 특정 자료에 대한 오답만 조회
    @Query("SELECT qa FROM QuizAttempt qa " +
           "JOIN qa.quiz q " +
           "WHERE qa.user.id = :userId " +
           "AND q.studyMaterial.id = :materialId " +
           "AND qa.isCorrect = false " +
           "ORDER BY qa.attemptedAt DESC")
    List<QuizAttempt> findByUserIdAndQuiz_StudyMaterial_IdAndIsCorrectFalse(
            @Param("userId") Long userId, 
            @Param("materialId") Long materialId);
    
    // 최근 오답 조회
    @Query("SELECT qa FROM QuizAttempt qa " +
           "WHERE qa.user.id = :userId " +
           "AND qa.isCorrect = false " +
           "ORDER BY qa.attemptedAt DESC " +
           "LIMIT :limit")
    List<QuizAttempt> findRecentWrongAttempts(
            @Param("userId") Long userId, 
            @Param("limit") int limit);
    
    // 특정 과목의 오답 조회
    @Query("SELECT qa FROM QuizAttempt qa " +
           "JOIN qa.quiz q " +
           "JOIN q.studyMaterial sm " +
           "WHERE qa.user.id = :userId " +
           "AND sm.course.id = :courseId " +
           "AND qa.isCorrect = false " +
           "ORDER BY qa.attemptedAt DESC " +
           "LIMIT :limit")
    List<QuizAttempt> findWrongAttemptsByCourse(
            @Param("userId") Long userId, 
            @Param("courseId") Long courseId, 
            @Param("limit") int limit);
    
    // 특정 기간 이후의 시도 조회
    List<QuizAttempt> findByUserIdAndAttemptedAtAfter(Long userId, LocalDateTime date);
    
    // 사용자가 특정 퀴즈를 시도한 횟수
    Integer countByUserIdAndQuizId(Long userId, Long quizId);
    
    // 사용자가 특정 자료의 퀴즈를 시도한 횟수
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa " +
           "JOIN qa.quiz q " +
           "WHERE qa.user.id = :userId " +
           "AND q.studyMaterial.id = :materialId")
    Integer countByUserIdAndQuiz_StudyMaterial_Id(
            @Param("userId") Long userId, 
            @Param("materialId") Long materialId);
    
    // 사용자의 모든 퀴즈 시도 조회
    List<QuizAttempt> findByUserId(Long userId);
    
    // 사용자의 모든 퀴즈 시도를 최신순으로 조회
    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);
    
    // 리더보드: 상위 사용자들의 점수 조회
    @Query("SELECT qa.user.id, SUM(qa.score), COUNT(qa), AVG(qa.score) " +
           "FROM QuizAttempt qa " +
           "GROUP BY qa.user.id " +
           "ORDER BY SUM(qa.score) DESC " +
           "LIMIT :limit")
    List<Object[]> findTopUsersByScore(@Param("limit") int limit);
    
    // 코스별 리더보드
    @Query("SELECT qa.user.id, SUM(qa.score), COUNT(qa), AVG(qa.score) " +
           "FROM QuizAttempt qa " +
           "JOIN qa.quiz q " +
           "JOIN q.studyMaterial sm " +
           "WHERE sm.course.id = :courseId " +
           "GROUP BY qa.user.id " +
           "ORDER BY SUM(qa.score) DESC " +
           "LIMIT :limit")
    List<Object[]> findTopUsersByCourse(@Param("courseId") Long courseId, @Param("limit") int limit);
    
    // 사용자의 통계 정보
    @Query("SELECT SUM(qa.score), COUNT(qa), AVG(qa.score) " +
           "FROM QuizAttempt qa " +
           "WHERE qa.user.id = :userId")
    Object[] findUserStats(@Param("userId") Long userId);
    
    // 사용자의 순위 계산
    @Query("SELECT COUNT(DISTINCT qa.user.id) " +
           "FROM QuizAttempt qa " +
           "GROUP BY qa.user.id " +
           "HAVING SUM(qa.score) > :userScore")
    Integer findUserRank(@Param("userScore") int userScore);
    
    // 사용자의 마지막 활동 시간
    @Query("SELECT MAX(qa.attemptedAt) " +
           "FROM QuizAttempt qa " +
           "WHERE qa.user.id = :userId")
    LocalDateTime findLastActivityByUserId(@Param("userId") Long userId);
}
