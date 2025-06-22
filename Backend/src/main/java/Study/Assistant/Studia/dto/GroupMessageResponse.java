package Study.Assistant.Studia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageResponse {
    private Long id;
    private String content;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private LocalDateTime timestamp;
    private Long groupId;
    private boolean isSystem;
}
