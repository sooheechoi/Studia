package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.response.LeaderboardResponse;
import Study.Assistant.Studia.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    
    private final LeaderboardService leaderboardService;
    
    /**
     * 전체 리더보드 조회
     */
    @GetMapping("/global")
    public ResponseEntity<List<LeaderboardResponse>> getGlobalLeaderboard(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        List<LeaderboardResponse> leaderboard = leaderboardService.getGlobalLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }
    
    /**
     * 코스별 리더보드 조회
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LeaderboardResponse.CourseLeaderboard>> getCourseLeaderboard(
            @PathVariable Long courseId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        List<LeaderboardResponse.CourseLeaderboard> leaderboard = 
                leaderboardService.getCourseLeaderboard(courseId, limit);
        return ResponseEntity.ok(leaderboard);
    }
    
    /**
     * 사용자 순위 조회
     */
    @GetMapping("/my-rank")
    public ResponseEntity<LeaderboardResponse> getMyRank() {
        LeaderboardResponse myRank = leaderboardService.getUserRank();
        return ResponseEntity.ok(myRank);
    }
}
