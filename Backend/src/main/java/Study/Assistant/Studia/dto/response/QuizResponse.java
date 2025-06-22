package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {
    private Long id;
    private String title;
    private MaterialSummaryResponse material;
    private int questionCount; // questions 필드 대신 개수만
    private int attempts;
    private Double bestScore; // 최고 점수 (백분율)
    private Integer totalAttempts; // 실제 총 시도 횟수
}
