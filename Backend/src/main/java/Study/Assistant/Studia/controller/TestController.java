package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final AIService aiService;
    
    @GetMapping("/hello")
    public Map<String, String> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Studia Backend!");
        response.put("status", "success");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Studia Backend");
        return response;
    }
    
    @GetMapping("/ai-test")
    public Map<String, Object> testAI() {
        Map<String, Object> response = new HashMap<>();
        
        // AI 요약 테스트
        String testContent = "This is a test content for AI summarization. It contains important information about Spring Boot and AI integration.";
        String summary = aiService.generateSummary(testContent);
        
        response.put("originalContent", testContent);
        response.put("summary", summary);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return response;
    }
}
