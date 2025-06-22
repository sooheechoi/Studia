package Study.Assistant.Studia.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class GroupInviteRequest {
    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
}
