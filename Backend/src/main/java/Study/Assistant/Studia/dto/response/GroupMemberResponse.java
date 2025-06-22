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
public class GroupMemberResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String profileImage;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private Boolean isOnline;
}
