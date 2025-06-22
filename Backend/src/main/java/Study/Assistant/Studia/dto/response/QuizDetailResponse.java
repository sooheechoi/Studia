package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailResponse {
    private Long id;
    private String title;
    private List<QuestionDetail> questions;
    private int totalQuestions;
    private int estimatedTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetail {
        private Long id;
        private String questionText;
        private String questionType;
        private String difficulty;
        private List<String> options;
        private int correctOption;
        private String explanation;
        private String hint;
        private String category;
    }
}
