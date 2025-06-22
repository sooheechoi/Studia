package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.StudyStatisticsDto;
import Study.Assistant.Studia.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping
    public ResponseEntity<StudyStatisticsDto> getUserStatistics(Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = getUserIdFromEmail(email);
            
            log.info("Fetching statistics for user: {}", email);
            StudyStatisticsDto statistics = statisticsService.getUserStatistics(userId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error fetching user statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<StudyStatisticsDto> getDashboardStatistics(Authentication authentication) {
        // Alias for the main statistics endpoint
        return getUserStatistics(authentication);
    }
    
    private Long getUserIdFromEmail(String email) {
        // This should be replaced with actual user lookup logic
        // For now, using a simple mapping
        switch (email) {
            case "test1@example.com":
                return 1L;
            case "test2@example.com":
                return 2L;
            case "test3@example.com":
                return 3L;
            default:
                throw new RuntimeException("User not found");
        }
    }
}
