package Study.Assistant.Studia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupRequest {
    private String name;
    private String description;
    private Integer maxMembers;
    private Boolean isPublic;
    private Long courseId;
}
