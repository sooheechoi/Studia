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
public class MaterialSummaryResponse {
    private Long id;
    private String title;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String summary;
    private String keyPoints;
    private String status;
    private Long courseId;
    private String courseName;
    private String className;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
