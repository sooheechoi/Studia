package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.request.StudyPlanRequest;
import Study.Assistant.Studia.dto.response.StudyPlanResponse;
import Study.Assistant.Studia.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8000"})
public class StudyPlanController {
    
    private final StudyPlanService studyPlanService;
    
    /**
     * Create a new study plan
     */
    @PostMapping
    public ResponseEntity<StudyPlanResponse> createStudyPlan(@RequestBody StudyPlanRequest request) {
        log.info("Creating study plan: {}", request);
        StudyPlanResponse response = studyPlanService.createStudyPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all study plans for the current user
     */
    @GetMapping
    public ResponseEntity<List<StudyPlanResponse>> getAllStudyPlans() {
        log.info("Getting all study plans");
        List<StudyPlanResponse> plans = studyPlanService.getAllStudyPlans();
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Get study plans by date range
     */
    @GetMapping("/range")
    public ResponseEntity<List<StudyPlanResponse>> getStudyPlansByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting study plans from {} to {}", startDate, endDate);
        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansByDateRange(startDate, endDate);
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Get a specific study plan
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudyPlanResponse> getStudyPlan(@PathVariable String id) {
        log.info("Getting study plan with id: {}", id);
        
        // 문자열 ID 처리 (todo-로 시작하는 경우)
        if (id.startsWith("todo-")) {
            log.warn("Client-side generated ID requested: {}. Not found.", id);
            throw new RuntimeException("Study plan not found");
        }
        
        // 숫자 ID 처리
        try {
            Long numericId = Long.parseLong(id);
            StudyPlanResponse plan = studyPlanService.getStudyPlan(numericId);
            return ResponseEntity.ok(plan);
        } catch (NumberFormatException e) {
            log.error("Invalid study plan ID format: {}", id);
            throw new RuntimeException("Invalid study plan ID format");
        }
    }
    
    /**
     * Update a study plan
     */
    @PutMapping("/{id}")
    public ResponseEntity<StudyPlanResponse> updateStudyPlan(
            @PathVariable String id,
            @RequestBody StudyPlanRequest request) {
        log.info("Updating study plan {} with data: {}", id, request);
        
        // 문자열 ID 처리 (todo-로 시작하는 경우)
        if (id.startsWith("todo-")) {
            log.warn("Client-side generated ID detected: {}. Creating new study plan instead.", id);
            // todo-로 시작하는 ID는 클라이언트에서 생성한 것이므로 새로 생성
            StudyPlanResponse response = studyPlanService.createStudyPlan(request);
            return ResponseEntity.ok(response);
        }
        
        // 숫자 ID 처리
        try {
            Long numericId = Long.parseLong(id);
            StudyPlanResponse response = studyPlanService.updateStudyPlan(numericId, request);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid study plan ID format: {}", id);
            throw new RuntimeException("Invalid study plan ID format");
        }
    }
    
    /**
     * Delete a study plan
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudyPlan(@PathVariable String id) {
        log.info("Deleting study plan with id: {}", id);
        
        // 문자열 ID 처리 (todo-로 시작하는 경우)
        if (id.startsWith("todo-")) {
            log.warn("Attempting to delete client-side generated ID: {}. Ignoring.", id);
            // 클라이언트 생성 ID는 서버에 없으므로 성공으로 처리
            return ResponseEntity.noContent().build();
        }
        
        // 숫자 ID 처리
        try {
            Long numericId = Long.parseLong(id);
            studyPlanService.deleteStudyPlan(numericId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            log.error("Invalid study plan ID format: {}", id);
            throw new RuntimeException("Invalid study plan ID format");
        }
    }
    
    /**
     * Delete recurring study plans
     */
    @DeleteMapping("/{id}/recurring")
    public ResponseEntity<Void> deleteRecurringStudyPlans(
            @PathVariable String id,
            @RequestParam(required = false) String groupId) {
        log.info("Deleting recurring study plans with id: {} and groupId: {}", id, groupId);
        
        // 문자열 ID 처리 (todo-로 시작하는 경우)
        if (id.startsWith("todo-")) {
            log.warn("Attempting to delete client-side generated recurring ID: {}. Ignoring.", id);
            return ResponseEntity.noContent().build();
        }
        
        // 숫자 ID 처리
        try {
            Long numericId = Long.parseLong(id);
            studyPlanService.deleteRecurringStudyPlans(numericId, groupId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            log.error("Invalid study plan ID format: {}", id);
            throw new RuntimeException("Invalid study plan ID format");
        }
    }
    
    /**
     * Update recurring study plans
     */
    @PutMapping("/{id}/recurring")
    public ResponseEntity<List<StudyPlanResponse>> updateRecurringStudyPlans(
            @PathVariable String id,
            @RequestParam(required = false) String groupId,
            @RequestBody StudyPlanRequest request) {
        log.info("Updating recurring study plans with id: {} and groupId: {}", id, groupId);
        
        // 문자열 ID 처리 (todo-로 시작하는 경우)
        if (id.startsWith("todo-")) {
            log.warn("Client-side generated ID detected for recurring update: {}. Creating new instead.", id);
            StudyPlanResponse response = studyPlanService.createStudyPlan(request);
            return ResponseEntity.ok(List.of(response));
        }
        
        // 숫자 ID 처리
        try {
            Long numericId = Long.parseLong(id);
            List<StudyPlanResponse> responses = studyPlanService.updateRecurringStudyPlans(numericId, groupId, request);
            return ResponseEntity.ok(responses);
        } catch (NumberFormatException e) {
            log.error("Invalid study plan ID format: {}", id);
            throw new RuntimeException("Invalid study plan ID format");
        }
    }
}
