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
public class StatsOverviewResponse {
    private int totalMaterials;
    private int processedMaterials;
    private int completedQuizzes;
    private int averageScore;
    private int studyStreak;
    private LocalDateTime lastActivity;
}
