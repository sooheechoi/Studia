package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptDetailResponse {
    private Long attemptId;
    private Long materialId;
    private String materialTitle;
    private LocalDateTime attemptedAt;
    private int duration; // in seconds
    private int score;
    private int totalQuestions;
    private double percentage;
    private List<QuestionAttemptDetail> questionAttempts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAttemptDetail {
        private Long questionId;
        private String questionText;
        private String questionType;
        private String difficulty;
        private List<String> options;
        private int correctOption;
        private int userSelectedOption;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;
        private String hint;
        private String category;
    }
}
