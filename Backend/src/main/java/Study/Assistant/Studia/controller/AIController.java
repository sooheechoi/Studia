package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8000")
public class AIController {
    
    private final AIService aiService;
    
    @PostMapping("/study-plan")
    public ResponseEntity<String> generateStudyPlan(@RequestBody Map<String, Object> request) {
        try {
            log.info("Generating study plan with request: {}", request);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> courses = (List<Map<String, Object>>) request.get("courses");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exams = (List<Map<String, Object>>) request.get("exams");
            
            String studyPlan = aiService.generateStudyPlan(courses, exams);
            
            return ResponseEntity.ok(studyPlan);
            
        } catch (Exception e) {
            log.error("Failed to generate study plan", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to generate study plan: " + e.getMessage());
        }
    }
}
