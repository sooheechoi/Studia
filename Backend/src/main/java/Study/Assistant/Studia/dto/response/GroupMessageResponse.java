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
public class GroupMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderImage;
    private String content;
    private String type;
    private Boolean isEdited;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private Long replyToId;
}
