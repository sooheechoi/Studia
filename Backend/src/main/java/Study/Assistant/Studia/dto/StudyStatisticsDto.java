package Study.Assistant.Studia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyStatisticsDto {
    
    // 전체 통계
    private int totalQuizzesTaken;
    private int totalQuestionsAnswered;
    private double overallAccuracy;
    private int totalStudyTime; // minutes
    private int currentStreak; // days
    private int longestStreak; // days
    
    // 주간/월간 통계
    private WeeklyStatistics weeklyStats;
    private MonthlyStatistics monthlyStats;
    
    // 과목별 통계
    private List<SubjectStatistics> subjectStats;
    
    // 최근 활동
    private List<RecentActivity> recentActivities;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyStatistics {
        private Map<String, Integer> dailyQuizCount; // day -> count
        private Map<String, Double> dailyAccuracy; // day -> accuracy
        private Map<String, Integer> dailyStudyTime; // day -> minutes
        private int totalQuizzes;
        private double averageAccuracy;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatistics {
        private Map<String, Integer> weeklyQuizCount; // week -> count
        private Map<String, Double> weeklyAccuracy; // week -> accuracy
        private Map<String, Integer> weeklyStudyTime; // week -> minutes
        private int totalQuizzes;
        private double averageAccuracy;
        private List<String> topSubjects; // Top 3 subjects
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectStatistics {
        private String subject;
        private int quizCount;
        private int questionCount;
        private double accuracy;
        private int studyTime; // minutes
        private LocalDateTime lastStudied;
        private double improvement; // percentage improvement over time
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String activityType; // QUIZ_TAKEN, SUMMARY_CREATED, etc.
        private String title;
        private String subject;
        private LocalDateTime timestamp;
        private Integer score; // nullable for non-quiz activities
    }
}
