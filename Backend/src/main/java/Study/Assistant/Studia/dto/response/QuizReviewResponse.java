package Study.Assistant.Studia.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizReviewResponse {
    private Long materialId;
    private Integer totalAttempts;
    private Integer correctAttempts;
    private Double overallAccuracy;
    private List<QuizStats> quizStatsList;
    private List<Long> weakQuizIds; // 취약 문제 ID 목록
    
    @Data
    @Builder
    public static class QuizStats {
        private Long quizId;
        private String question;
        private String difficulty;
        private Integer totalAttempts;
        private Integer correctAttempts;
        private Double accuracy;
        private LocalDateTime lastAttemptedAt;
    }
}
