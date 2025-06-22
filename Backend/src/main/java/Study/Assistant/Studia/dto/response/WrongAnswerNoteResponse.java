package Study.Assistant.Studia.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WrongAnswerNoteResponse {
    private Long attemptId;
    private Long quizId;
    private Long materialId;
    private String materialTitle;
    private String question;
    private String userAnswer;
    private String correctAnswer;
    private String explanation;
    private String difficulty;
    private LocalDateTime attemptedAt;
    private Integer attemptCount; // 해당 문제 시도 횟수
}
