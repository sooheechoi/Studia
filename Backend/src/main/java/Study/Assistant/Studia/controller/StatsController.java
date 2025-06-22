package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.response.StatsOverviewResponse;
import Study.Assistant.Studia.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    
    private final StatsService statsService;
    
    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview() {
        StatsOverviewResponse stats = statsService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}
