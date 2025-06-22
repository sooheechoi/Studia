package Study.Assistant.Studia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private Long id;
    private String name;
    private String email;
    private String university;
    private String major;
    private String profileImage;
    private Boolean isFriend;
    private Boolean hasPendingRequest;
    private Boolean requestSentByMe;
    private Integer mutualFriendsCount;
}
