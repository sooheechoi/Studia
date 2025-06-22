package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizHistoryResponse {
    private Long attemptId;
    private LocalDateTime attemptedAt;
    private int score;
    private int totalQuestions;
    private double percentage;
    private int duration; // 초 단위
    private String materialTitle;
}
