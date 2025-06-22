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
public class StudyGroupResponse {
    private Long id;
    private String name;
    private String description;
    private Long courseId;
    private String courseName;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private Integer maxMembers;
    private Boolean isPublic;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String nextMeeting;  // For future implementation
}
