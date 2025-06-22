package Study.Assistant.Studia.dto;

import lombok.Data;

@Data
public class GroupMessageRequest {
    private String content;
    private Long groupId;
}
