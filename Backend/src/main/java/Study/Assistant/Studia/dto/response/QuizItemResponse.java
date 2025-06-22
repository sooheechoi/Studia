package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizItemResponse {
    private Long id;
    private String question;
    private String questionType;
    private String difficulty;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private String category;
    private String hint;
}
