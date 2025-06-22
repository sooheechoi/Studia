package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private Long userId;
    private String username;
    private int totalScore;
    private int totalQuizzes;
    private double averageScore;
    private int rank;
    private LocalDateTime lastActivityAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseLeaderboard {
        private Long courseId;
        private String courseName;
        private Long userId;
        private String username;
        private int totalScore;
        private int quizzesTaken;
        private double averageScore;
        private int rank;
    }
}
