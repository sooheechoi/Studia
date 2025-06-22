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
public class FriendResponse {
    private Long id;
    private String name;
    private String email;
    private String university;
    private String major;
    private String profileImage;
    private String statusMessage;
    private Boolean isOnline;
    private LocalDateTime friendSince;
    
    // For pending requests
    private Long requestId;
    private LocalDateTime requestedAt;
}
