package Study.Assistant.Studia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageRequest {
    private String content;
    private String type = "TEXT"; // TEXT, FILE, IMAGE, etc.
    private String metadata; // JSON string for additional data
}
